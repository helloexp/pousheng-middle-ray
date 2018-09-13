package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.common.utils.batchhandle.AbnormalWarehouseStockRuleRecord;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.ShopWarehouseRuleClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.ShopWarehouseStockRule;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/8/9
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_WAREHOUSE_SKU_RULE)
@Service
@Slf4j
public class ImportWarehouseStockRuleService implements CompensateBizService {

    @RpcConsumer
    private MappingReadService mappingReadService;

    @RpcConsumer
    private ShopWarehouseRuleClient warehouseShopRuleClient;

    @RpcConsumer
    private WarehouseRulesClient warehouseRulesClient;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private static final Integer DEFAULT_PAGE_SIZE = 10;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import warehouse stock rule start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportWarehouseStockRuleService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportWarehouseStockRuleService.doProcess context is null");
            return;
        }
        poushengCompensateBiz = makeRules(poushengCompensateBiz);
        poushengCompensateBizWriteService.update(poushengCompensateBiz);
    }

    private void appendErrorToExcel(ExcelExportHelper<AbnormalWarehouseStockRuleRecord> helper, String[] strs, String error) {
        AbnormalWarehouseStockRuleRecord abnormalSkuStockRuleRecord = new AbnormalWarehouseStockRuleRecord();
        if (!StringUtils.isEmpty(strs[0])) {
            abnormalSkuStockRuleRecord.setWarehouseCode(strs[0].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[1])) {
            abnormalSkuStockRuleRecord.setSafeStock(strs[1].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[2])) {
            abnormalSkuStockRuleRecord.setRatio(strs[2].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[3])) {
            abnormalSkuStockRuleRecord.setJitStock(strs[3].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[4])) {
            abnormalSkuStockRuleRecord.setStatus(strs[4].replace("\"", ""));
        }
        abnormalSkuStockRuleRecord.setReason(error);
        helper.appendToExcel(abnormalSkuStockRuleRecord);
    }

    private String[] createStrs(ShopWarehouseStockRule rule) {
        String[] strs = new String[10];
        strs[0] = rule.getWarehouseId().toString();
        strs[1] = rule.getSafeStock().toString();
        strs[2] = rule.getRatio().toString();
        strs[3] = rule.getJitStock() == null ? null : rule.getJitStock().toString();
        strs[4] = rule.getStatus().toString();
        return strs;
    }

    private PoushengCompensateBiz makeRules(PoushengCompensateBiz poushengCompensateBiz) {
        List<ShopWarehouseStockRule> rules = Lists.newArrayList();
        SkuStockRuleImportInfo info = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), SkuStockRuleImportInfo.class);
        String filePath = info.getFilePath();
        Long openShopId = info.getOpenShopId();
        ExcelExportHelper<AbnormalWarehouseStockRuleRecord> helper = ExcelExportHelper.newExportHelper(AbnormalWarehouseStockRuleRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(filePath);
        for (int i = 1; i < list.size(); i++) {
            String[] strs = list.get(i);
            if (Strings.isNullOrEmpty(strs[0]) || "\"\"".equals(strs[0])) {
                appendErrorToExcel(helper, strs, "请输入正确的仓库编码");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[1]) || "\"\"".equals(strs[1]) || !strs[1].matches("[0-9]+")) {
                appendErrorToExcel(helper, strs, "请输入正确的保障库存");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[2]) || "\"\"".equals(strs[2]) || !strs[2].matches("[0-9]+") || Integer.valueOf(strs[2]) < 0 || Integer.valueOf(strs[2]) > 100) {
                appendErrorToExcel(helper, strs, "请输入正确的推送比例");
                continue;
            }
            if (!Strings.isNullOrEmpty(strs[3]) && !"\"\"".equals(strs[3]) && !strs[3].matches("[0-9]+")) {
                appendErrorToExcel(helper, strs, "请输入正确的虚拟库存");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[4]) || "\"\"".equals(strs[4]) ||
                    !strs[4].matches("^-?\\d+$") || (Integer.valueOf(strs[4]) != -1 && Integer.valueOf(strs[4]) != 1)) {
                appendErrorToExcel(helper, strs, "请输入正确的状态");
                continue;
            }
            String skuCode = strs[0].replace("\"", "");
            try {
                Response<List<Long>> resp = warehouseRulesClient.findWarehouseIdsByShopId(openShopId);
                //查询默认发货仓列表失败
                if (!resp.isSuccess()) {
                    throw new JsonResponseException(resp.getError());
                }
                WarehouseDTO warehouseDTO;
                try {
                    warehouseDTO = warehouseCacher.findByCode(strs[0]);
                } catch (Exception e) {
                    appendErrorToExcel(helper, strs, "未找到对应仓库");
                    continue;
                }
                if (!resp.getResult().contains(warehouseDTO.getId())) {
                    appendErrorToExcel(helper, strs, "仓库不在默认发货仓规则内");
                    continue;
                }
                ShopWarehouseStockRule rule = new ShopWarehouseStockRule();
                rule.setWarehouseId(warehouseDTO.getId());
                rule.setShopId(openShopId);
                rule.setSafeStock(Long.valueOf(strs[1]));
                rule.setRatio(Integer.valueOf(strs[2]));
                rule.setJitStock(StringUtils.isEmpty(strs[3]) ? null : Long.valueOf(strs[3]));
                rule.setStatus(Integer.valueOf(strs[4]));
                rules.add(rule);

            } catch (Exception jre) {
                appendErrorToExcel(helper, strs, "处理失败");
                log.error("import make item group sku code:{} flag fail, cause:{}",
                        skuCode, Throwables.getStackTraceAsString(jre));
            }

        }
        if (!StringUtils.isEmpty(rules)) {
            try {
                for (ShopWarehouseStockRule rule : rules) {
                    Response<Boolean> response = warehouseShopRuleClient.createOrUpdate(rule);
                    if (!response.isSuccess()) {
                        appendErrorToExcel(helper, createStrs(rule), response.getError());
                    }
                }
            } catch (Exception e) {
                log.error("import warehouse shop sku rule abnormalty cause by {}", e);
                poushengCompensateBiz.setLastFailedReason(e.getMessage());
                poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.SUCCESS.name());
            }

        }
        if (helper.size() > 0) {
            String url = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(url);
            poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.SUCCESS.name());
            log.error("import shop  warehouse rule abnormality");
        }
        return poushengCompensateBiz;
    }

}
