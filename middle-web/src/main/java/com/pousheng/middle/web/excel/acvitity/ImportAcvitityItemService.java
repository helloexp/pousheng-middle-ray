package com.pousheng.middle.web.excel.acvitity;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Desc   活动赠品，导入商品
 * @Author GuoFeng
 * @Date 2019/6/12
 */
@Data
@Slf4j
@Service
public class ImportAcvitityItemService {

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    @Autowired
    private InventoryClient inventoryClient;

    public static final String templateName = "ps_search.mustache";

    public static final int page_size = 1000;

    /**
     * 导入文件
     * @return
     */
    public Response<AcvitityItemResponseBean> importFile(ActivityItemRequest request) {
        AcvitityItemResponseBean responseBean = new AcvitityItemResponseBean();
        try {
            String filePath = request.getFilePath();
            if (StringUtils.isEmpty(filePath)) {
                String errorMsg = "文件地址不存在:" + request.getFilePath();
                log.error(errorMsg);
                return Response.fail(errorMsg);
            }
            //错误文件
            ExcelExportHelper<ActivityItemExcelFaildBean> errorExcel = ExcelExportHelper.newExportHelper(ActivityItemExcelFaildBean.class);

            Set<String> data = HandlerFileUtil.getInstance().handleAcvitityItemExcel(filePath);

            if (data.size() > 0) {
                log.info("解析到商品货号:{}", data);
                //保存ES搜索到的数据
                List<SearchSkuTemplate> searchData = Lists.newArrayList();
                for (String code : data) {
                    Map<String,String> params = Maps.newHashMap();
                    params.put("spuCode", code);
                    params.put("sort","0_0_0_2");
                    //11 : 从ES最多取10000条数据
                    for (int i = 1; i < 11; i++) {
                        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response =skuTemplateSearchReadService.searchWithAggs(i, page_size, templateName, params, SearchSkuTemplate.class);
                        log.info("货号:{},ES响应信息:{}", code, JSONObject.toJSONString(response));
                        if (response.isSuccess()) {
                            SearchedItemWithAggs<SearchSkuTemplate> responseData = response.getResult();
                            if (responseData != null) {
                                Paging<SearchSkuTemplate> entities = responseData.getEntities();
                                if (entities != null && entities.getData() != null && entities.getData().size() > 0) {
                                    searchData.addAll(entities.getData());
                                    Long total = entities.getTotal();
                                    if (total < page_size) {
                                        break;
                                    }
                                } else {
                                    //没有数据
                                    ActivityItemExcelFaildBean activityItemExcelFaildBean = new ActivityItemExcelFaildBean();
                                    activityItemExcelFaildBean.setItemCode(code);
                                    activityItemExcelFaildBean.setMsg("货号不存在");
                                    errorExcel.appendToExcel(activityItemExcelFaildBean);
                                    log.info("responseData.getEntities()没有数据");
                                    break;
                                }
                            } else {
                                //没有数据
                                ActivityItemExcelFaildBean activityItemExcelFaildBean = new ActivityItemExcelFaildBean();
                                activityItemExcelFaildBean.setItemCode(code);
                                activityItemExcelFaildBean.setMsg("货号不存在");
                                errorExcel.appendToExcel(activityItemExcelFaildBean);
                                log.info("responseData没有数据");
                                break;
                            }
                        } else {
                            //查询失败
                            ActivityItemExcelFaildBean activityItemExcelFaildBean = new ActivityItemExcelFaildBean();
                            activityItemExcelFaildBean.setItemCode(code);
                            activityItemExcelFaildBean.setMsg("查询失败");
                            errorExcel.appendToExcel(activityItemExcelFaildBean);
                            log.info("ES查询失败,货号:{}", code);
                            break;
                        }
                    }
                }

                //searchData转换
                if (searchData.size() > 0) {
                    List<Long> ids = searchData.stream().map(SearchSkuTemplate::getId).collect(Collectors.toList());
                    Response<List<SkuTemplate>> skuTemplates = skuTemplateSearchReadService.findByIds(ids);
                    if (skuTemplates.isSuccess()) {
                        List<SkuTemplate> list = skuTemplates.getResult();
                        responseBean.setResultData(list);
                    } else {
                        String errorMsg = "数据库查询商品出错";
                        log.error(errorMsg);
                        return Response.fail(errorMsg);
                    }
                } else {
                    log.info("没有待处理的ES数据");
                }

                //错误文件
                if (errorExcel.size() > 0) {
                    File file = errorExcel.transformToFile();
                    String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                    responseBean.setFaildFileUrl(abnormalUrl);
                }
            }
        } catch (Exception e) {
            String errorMsg = "导入活动商品出错:" + e.getMessage();
            log.error(errorMsg);
            e.printStackTrace();
            return Response.fail(errorMsg);
        }

        return Response.ok(responseBean);
    }

}
