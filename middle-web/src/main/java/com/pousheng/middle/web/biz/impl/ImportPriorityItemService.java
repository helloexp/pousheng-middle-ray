package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Strings;
import com.pousheng.middle.common.utils.batchhandle.AbnormalPriorityItemRecord;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.open.api.dto.WarehouseRulePriorityImportInfo;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesItemClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.WarehouseRuleDto;
import com.pousheng.middle.warehouse.dto.WarehouseRuleItemDto;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemWriteService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityReadService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/8/9
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE_PRIORITY_ITEM)
@Service
@Slf4j
public class ImportPriorityItemService implements CompensateBizService {

    @RpcConsumer
    private WarehouseRulesItemClient warehouseRulesItemClient;

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @RpcConsumer
    private WarehouseRulePriorityItemWriteService warehouseRulePriorityItemWriteService;

    @RpcConsumer
    private WarehouseRulePriorityItemReadService warehouseRulePriorityItemReadService;

    @RpcConsumer
    private WarehouseRulePriorityReadService warehouseRulePriorityReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import warehouse rule priority item  start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportPriorityItemService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportPriorityItemService.doProcess context is null");
            return;
        }
        poushengCompensateBiz = makeRules(poushengCompensateBiz);
        poushengCompensateBizWriteService.update(poushengCompensateBiz);
    }

    private void appendErrorToExcel(ExcelExportHelper<AbnormalPriorityItemRecord> helper, String[] strs, String error) {
        AbnormalPriorityItemRecord AbnormalPriorityItemRecord = new AbnormalPriorityItemRecord();
        if (!StringUtils.isEmpty(strs[0])) {
            AbnormalPriorityItemRecord.setWatrehouseCode(strs[0].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[1])) {
            AbnormalPriorityItemRecord.setPriority(strs[1].replace("\"", ""));
        }
        AbnormalPriorityItemRecord.setReason(error);
        helper.appendToExcel(AbnormalPriorityItemRecord);
    }


    private PoushengCompensateBiz makeRules(PoushengCompensateBiz poushengCompensateBiz) {
        WarehouseRulePriorityImportInfo info = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), WarehouseRulePriorityImportInfo.class);
        String filePath = info.getFilePath();
        Response<WarehouseRulePriority> resp = warehouseRulePriorityReadService.findById(info.getPriorityId());
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        Long ruleId = resp.getResult().getRuleId();
        Response<WarehouseRuleDto> itemResp = warehouseRulesItemClient.findByRuleId(ruleId);
        //不存在记录日志
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        List<Long> warehouseIds = itemResp.getResult().getWarehouseRuleItemDtos().stream().map(WarehouseRuleItemDto::getWarehouseId).collect(Collectors.toList());
        ExcelExportHelper<AbnormalPriorityItemRecord> helper = ExcelExportHelper.newExportHelper(AbnormalPriorityItemRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(filePath);
        for (int i = 1; i < list.size(); i++) {
            String[] strs = list.get(i);
            if (Strings.isNullOrEmpty(strs[0]) || "\"\"".equals(strs[0])) {
                appendErrorToExcel(helper, strs, "请输入正确的公司编码");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[1]) || "\"\"".equals(strs[1])) {
                appendErrorToExcel(helper, strs, "请输入正确的仓库外码");
                continue;
            }
            Long warehouseId;
            try {
                //检查仓库存不存在
                warehouseId = warehouseCacher.findByOutCodeAndBizId(strs[1], strs[0]).getId();
            } catch (ServiceException e) {
                appendErrorToExcel(helper, strs, "仓库不存在，请输入正确的仓库编码");
                continue;
            }
            if (!Strings.isNullOrEmpty(strs[2]) && !"\"\"".equals(strs[2]) && !strs[2].matches("[0-9]+")) {
                appendErrorToExcel(helper, strs, "请输入正确的优先级");
                continue;
            }
            try {
                if (!warehouseIds.contains(warehouseId)) {
                    appendErrorToExcel(helper, strs, "该仓库不在发货仓规则内");
                    continue;
                }
                WarehouseRulePriorityItem item = new WarehouseRulePriorityItem();
                item.setPriorityId(info.getPriorityId());
                item.setWarehouseId(warehouseId);
                if (!StringUtils.isEmpty(strs[2])) {
                    item.setPriority(Integer.parseInt(strs[2]));
                }
                Response<WarehouseRulePriorityItem> warehouseRulePriorityItemResponse = warehouseRulePriorityItemReadService.findByEntity(item);
                if (!warehouseRulePriorityItemResponse.isSuccess()) {
                    appendErrorToExcel(helper, strs, warehouseRulePriorityItemResponse.getError());
                    continue;
                }
                //若记录不存在 则去创建，若优先级不存在则忽略
                if (warehouseRulePriorityItemResponse.getResult() == null) {
                    if (item.getPriority() == null) {
                        continue;
                    }
                    Response<Long> createResp = warehouseRulePriorityItemWriteService.create(item);
                    if (!createResp.isSuccess()) {
                        appendErrorToExcel(helper, strs, createResp.getError());
                        continue;
                    }
                } else {
                    //若存在，且优先级为空则删除，否则更新
                    item.setId(warehouseRulePriorityItemResponse.getResult().getId());
                    if (item.getPriority() == null) {
                        Response<Boolean> delResp = warehouseRulePriorityItemWriteService.deleteById(item.getId());
                        if (!delResp.isSuccess()) {
                            appendErrorToExcel(helper, strs, delResp.getError());
                        }
                        continue;
                    }
                    Response<Boolean> updateResp = warehouseRulePriorityItemWriteService.update(item);
                    if (!updateResp.isSuccess()) {
                        appendErrorToExcel(helper, strs, updateResp.getError());
                    }
                }
            } catch (Exception jre) {
                appendErrorToExcel(helper, strs, "处理失败");
            }

        }

        if (helper.size() > 0) {
            String url = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(url);
            poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.SUCCESS.name());
            log.error("import warehouse rule item detail abnormality");
        }
        return poushengCompensateBiz;
    }
}
