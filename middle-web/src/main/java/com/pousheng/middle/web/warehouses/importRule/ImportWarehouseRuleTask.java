package com.pousheng.middle.web.warehouses.importRule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.dto.RuleItemBatchRequest;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.NoTryException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.warehouses.component.StockSendRuleExcelParser;
import com.pousheng.middle.web.warehouses.dto.*;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Desc   默认发货仓规则导入
 * @Author GuoFeng
 * @Date 2019/8/5
 */

@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE)
@Service
@Slf4j
public class ImportWarehouseRuleTask implements CompensateBizService {

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Autowired
    private WarehouseRulesClient warehouseRulesClient;

    @Autowired
    private StockSendRuleExcelParser stockSendRuleExcelParser;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        log.info("ImportWarehouseRuleTask==> start to process warehouse rule import task ID:{}", bizID);
        poushengCompensateBiz = processFile(poushengCompensateBiz);
        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setUpdatedAt(new Date());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }
        log.info("ImportWarehouseRuleTask==> process warehouse rule import task ID:{} finished", bizID);
    }

    public PoushengCompensateBiz processFile(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        try {
            String context = poushengCompensateBiz.getContext();
            if (StringUtils.isEmpty(context)) {
                log.error("ImportAftersaleOrderTask==> task ID:{} content is empty !", bizID);
                throw new BizException("任务参数为空");
            }

            StockSendImportRequest request = JSONObject.parseObject(context, StockSendImportRequest.class);

//            List<Long> shopIds = Lists.newArrayList();
//            for (Long shopId : request.getShopIds()) {
//                OpenShop shop = openShopCacher.findById(shopId);
//                if (shop != null) {
//                    shopIds.add(shop.getId());
//                }
//            }
//            if (shopIds.isEmpty()) {
//                String msg = "您要导入的门店不存在，已终止导入";
//                log.info(msg);
//                poushengCompensateBiz.setLastFailedReason(msg);
//                return poushengCompensateBiz;
//            }

            log.info("start parse excel: {}", request.getFilePath());
            List<StockSendImprtDTO> data = stockSendRuleExcelParser.parse(request.getFilePath());
            if (data.size() > 0) {
                //错误数据
                List<StockSendImprtDTO> errorData = data.stream().filter(StockSendImprtDTO::isHasError).collect(Collectors.toList());
                //正确的数据再过滤掉重复的数据
                List<StockSendImprtDTO> correctDataUnFilter = data.stream().filter(s -> !s.isHasError()).collect(Collectors.toList());
                Map<String, StockSendImprtDTO> correctDataMap = Maps.newHashMap();
                for (StockSendImprtDTO ss : correctDataUnFilter) {
                    correctDataMap.put(ss.getCompanyCode() + ss.getOutCode(), ss);
                }
                Collection<StockSendImprtDTO> correctData = correctDataMap.values();
                Set<Long> warehouseIds = correctData.stream()
                        .map(StockSendImprtDTO::getWarehouseId)
                        .collect(Collectors.toSet());

                //生成下载文件
                List<WarehouseImportFileErrorBean> errorDataToDownload = Lists.newArrayList();
                ExcelExportHelper<WarehouseImportFileErrorBean> errorExcel = ExcelExportHelper.newExportHelper(WarehouseImportFileErrorBean.class);
                if (warehouseIds.size() > 0) {
                    Map<Long, StockSendImprtDTO> warehouseIdToStockSendImprtDTOMap = correctData.stream().collect(Collectors.toMap(s -> s.getWarehouseId(), s -> s));
                    log.info("about to import {} for shops: {}", warehouseIds, request.getShopIds());
                    String response = warehouseRulesClient.batch(new RuleItemBatchRequest(request.getDelete(), request.getIsAll(), request.getShopIds(), new ArrayList<>(warehouseIds)));
                    WarehouseResultResponse warehouseResultsResponse = JSON.parseObject(response, WarehouseResultResponse.class);
                    if (warehouseResultsResponse.isSuccess()) {
                        List<WarehouseResult> warehouseResults = warehouseResultsResponse.getData();
                        //处理错误的数据
                        if (warehouseResults != null && warehouseResults.size() > 0) {
                            for (WarehouseResult warehouseResult : warehouseResults) {
                                WarehouseImportFileErrorBean errorBean = new WarehouseImportFileErrorBean();
                                StockSendImprtDTO stockSendImprtDTO = warehouseIdToStockSendImprtDTOMap.get(warehouseResult.getWarehouseId());
                                errorBean.setCompanyCode(stockSendImprtDTO.getCompanyCode());
                                errorBean.setOutCode(stockSendImprtDTO.getOutCode());
                                errorBean.setMsg(warehouseResult.getMsg());

                                errorDataToDownload.add(errorBean);
                            }
                        }
                    } else {
                        poushengCompensateBiz.setLastFailedReason(warehouseResultsResponse.getMsg());
                        return poushengCompensateBiz;
                    }
                }
                //参数缺失的数据
                if (errorData.size() > 0) {
                    for (StockSendImprtDTO stock : errorData) {
                        WarehouseImportFileErrorBean errorBean = new WarehouseImportFileErrorBean();
                        errorBean.setMsg(stock.getErrorMsg());
                        errorBean.setOutCode(stock.getOutCode());
                        errorBean.setCompanyCode(stock.getCompanyCode());

                        errorDataToDownload.add(errorBean);
                    }
                }

                if (errorDataToDownload.size() > 0) {
                    errorExcel.appendToExcel(errorDataToDownload);
                    File file = errorExcel.transformToFile();
                    String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                    poushengCompensateBiz.setLastFailedReason(abnormalUrl);
                }
            }
        } catch (Exception e) {
            log.error("处理默认发货仓导入出错, 任务ID:{}, 错误:{}", bizID, e.getMessage());
            e.printStackTrace();
            poushengCompensateBiz.setLastFailedReason(e.getMessage());
        }
        return poushengCompensateBiz;
    }
}
