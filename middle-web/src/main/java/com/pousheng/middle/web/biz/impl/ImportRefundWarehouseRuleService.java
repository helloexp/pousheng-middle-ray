package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.SettingShopRefundWarehouseRule;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.RefundWarehouseRules;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.order.service.RefundWarehouseRulesReadService;
import com.pousheng.middle.order.service.RefundWarehouseRulesWriteService;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.shop.component.OpenShopLogic;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.common.model.Response;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/9/3
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_SHOP_REFUND_TARGET_WAREHOUSE_RULE)
@Service
@Slf4j
public class ImportRefundWarehouseRuleService implements CompensateBizService {
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private UploadFileComponent uploadFileComponent;
    @Autowired
    private OpenShopLogic openShopLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWarehouseRulesWriteService refundWarehouseRulesWriteService;
    @Autowired
    private RefundWarehouseRulesReadService refundWarehouseRulesReadService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import RefundWarehouseRule start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportRefundWarehouseRuleService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportRefundWarehouseRuleService.doProcess context is null");
            return;
        }
        poushengCompensateBiz = doRefundWarehouseRule(poushengCompensateBiz);

        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }
        log.info("import RefundWarehouseRule end ....,poushengCompensateBiz is {}", poushengCompensateBiz);
    }

    private PoushengCompensateBiz doRefundWarehouseRule(PoushengCompensateBiz poushengCompensateBiz) {
        String url = poushengCompensateBiz.getContext();
        ExcelExportHelper<SettingShopRefundWarehouseRule> helper = ExcelExportHelper.newExportHelper(SettingShopRefundWarehouseRule.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(url);
        for (int i = 1; i < list.size(); i++) {
            String[] strs = list.get(i);
            String failReason = "";
            try {
                if (!StringUtils.hasText(strs[0])) {
                    failReason = "销售店铺代码不可为空";
                    continue;
                }
                if (!StringUtils.hasText(strs[1])) {
                    failReason = "发货仓账套不可为空";
                    continue;
                }
                if (!StringUtils.hasText(strs[2])) {
                    failReason = "退货仓代码不可为空";
                    continue;
                }
                String saleShopCode = strs[0].replace("\"", "").trim();
                String shipmentCompanyId = strs[1].replace("\"", "").trim();
                String warehouseCode = strs[2].replace("\"", "").trim();

                String[] shopStrs = saleShopCode.split("-");
                if(shopStrs.length != 2){
                    failReason = "销售店铺代码格式填写错误";
                    continue; 
                }
                String[] warehouseStrs = warehouseCode.split("-");
                if(warehouseStrs.length != 2){
                    failReason = "退货仓代码格式填写错误";
                    continue;
                }
                List<OpenShop> shopLists = openShopLogic.searchByOuterIdAndBusinessId(shopStrs[1],shopStrs[0]);
                if(CollectionUtils.isEmpty(shopLists)){
                    failReason = "不存在此销售店铺，请填写正确信息";
                    continue;
                }
                try {
                    WarehouseDTO housedto = refundReadLogic.findWarehouseByCodeandCompanyId(warehouseCode);
                } catch (Exception e1){
                    failReason = "退货仓查询失败，请填写正确信息";
                    continue;
                }
                
                if(StringUtils.hasText(failReason)){
                    continue;
                }
                for(OpenShop s: shopLists){
                    RefundWarehouseRules rule = new RefundWarehouseRules();
                    rule.setShopId(s.getId());
                    rule.setOrderShopCode(saleShopCode);
                    rule.setOrderShopName(s.getShopName());
                    rule.setShipmentCompanyId(shipmentCompanyId);
                    rule.setRefundWarehouseCode(warehouseCode);
                    Response<RefundWarehouseRules> refundHouseRule = refundWarehouseRulesReadService.findByShopIdAndShipmentCompanyId(s.getId(),shipmentCompanyId);
                    if(refundHouseRule.isSuccess()){
                        if(refundHouseRule.getResult() != null){
                            // 存在记录则更新，否则新增
                            refundWarehouseRulesWriteService.updateRefundWarehouseRules(rule);
                        } else {
                            rule.setCreatedAt(new Date());
                            refundWarehouseRulesWriteService.createRefundWarehouseRules(rule);
                        }
                    } else {
                        failReason = "查询RefundWarehouseRules表异常失败";
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("failed to import shopRefundWarehouseRule url:{}, cause:{}", url, Throwables.getStackTraceAsString(e));
                failReason = "系统异常";
            } finally {
                if (StringUtils.hasText(failReason)) {
                    SettingShopRefundWarehouseRule record = new SettingShopRefundWarehouseRule();
                    if (!StringUtils.isEmpty(strs[0])) {
                        record.setOrderShopCode(strs[0].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(strs[1])) {
                        record.setShipmentCompanyId(strs[1].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(strs[2])) {
                        record.setRefundWarehouseCode(strs[2].replace("\"", ""));
                    }
                    record.setFailReason(failReason);
                    helper.appendToExcel(record);
                }
            }
        }
        if (helper.size() > 0) {
            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
            poushengCompensateBiz.setUpdatedAt(DateTime.now().toDate());
        }
        return poushengCompensateBiz;
    }
}
