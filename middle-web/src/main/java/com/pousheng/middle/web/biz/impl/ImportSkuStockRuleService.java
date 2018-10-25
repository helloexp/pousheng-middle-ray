package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.common.utils.batchhandle.AbnormalSkuStockRuleRecord;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.companent.ShopWarehouseSkuRuleClient;
import com.pousheng.middle.warehouse.dto.ShopWarehouseSkuStockRule;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @author zhaoxw
 * @date 2018/8/9
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_SHOP_SKU_RULE)
@Service
@Slf4j
public class ImportSkuStockRuleService implements CompensateBizService {

    @RpcConsumer
    private MappingReadService mappingReadService;

    @RpcConsumer
    private ShopWarehouseSkuRuleClient warehouseShopSkuRuleClient;

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private GroupRuleCacherProxy groupRuleCacherProxy;

    @Autowired
    private UploadFileComponent uploadFileComponent;

    private static final Integer DEFAULT_PAGE_SIZE = 10;

    private static final String TEMPLATE_NAME = "ps_search.mustache";


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import sku stock rule start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportSkuStockRule.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportSkuStockRule.doProcess context is null");
            return;
        }
        poushengCompensateBiz = makeRules(poushengCompensateBiz);
        poushengCompensateBizWriteService.update(poushengCompensateBiz);
        log.info("import sku stock rule end ....,poushengCompensateBiz is {}", poushengCompensateBiz);
    }

    private void appendErrorToExcel(ExcelExportHelper<AbnormalSkuStockRuleRecord> helper, String[] strs, String error) {
        AbnormalSkuStockRuleRecord abnormalSkuStockRuleRecord = new AbnormalSkuStockRuleRecord();
        if (!StringUtils.isEmpty(strs[0])) {
            abnormalSkuStockRuleRecord.setSkuCode(strs[0].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[1])) {
            abnormalSkuStockRuleRecord.setSafeStock(strs[1].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[2])) {
            abnormalSkuStockRuleRecord.setRatio(strs[2].replace("\"", ""));
        }
        if (!StringUtils.isEmpty(strs[3])) {
            abnormalSkuStockRuleRecord.setStatus(strs[3].replace("\"", ""));
        }
        abnormalSkuStockRuleRecord.setReason(error);
        helper.appendToExcel(abnormalSkuStockRuleRecord);
    }

    private String[] createStrs(ShopWarehouseSkuStockRule rule) {
        String[] strs = new String[10];
        strs[0] = rule.getSkuCode();
        strs[1] = rule.getSafeStock().toString();
        strs[2] = rule.getRatio().toString();
        strs[3] = rule.getStatus().toString();
        return strs;
    }

    private PoushengCompensateBiz makeRules(PoushengCompensateBiz poushengCompensateBiz) {
        List<ShopWarehouseSkuStockRule> rules = Lists.newArrayList();
        SkuStockRuleImportInfo info = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), SkuStockRuleImportInfo.class);
        String filePath = info.getFilePath();
        Long openShopId = info.getOpenShopId();
        Long warehouseId = info.getWarehouseId();
        OpenShop openShop = openShopCacher.findById(openShopId);
        Boolean isYunJuJIT = Objects.equals(openShop.getChannel(), MiddleChannel.YUNJUJIT.getValue());
        Map<String, String> params =new HashMap<>();
        List<Long> groupIds = groupRuleCacherProxy.findByShopId(openShopId);
        params.put("groupIds", Joiners.COMMA.join(groupIds));
        ExcelExportHelper<AbnormalSkuStockRuleRecord> helper = ExcelExportHelper.newExportHelper(AbnormalSkuStockRuleRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(filePath);
        for (int i = 1; i < list.size(); i++) {
            String[] strs = list.get(i);
            if (Strings.isNullOrEmpty(strs[0]) || "\"\"".equals(strs[0])) {
                appendErrorToExcel(helper, strs, "请输入正确的货品条码");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[1]) || "\"\"".equals(strs[1]) || !strs[1].matches("[0-9]+")) {
                appendErrorToExcel(helper, strs, "请输入正确的保障库存");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[2]) || "\"\"".equals(strs[2]) || !strs[2].matches("[0-9]+") || Integer.valueOf(strs[2]) < 1 || Integer.valueOf(strs[2]) > 100) {
                appendErrorToExcel(helper, strs, "请输入正确的推送比例");
                continue;
            }
            if (Strings.isNullOrEmpty(strs[3]) || "\"\"".equals(strs[3]) ||
                    !strs[3].matches("^-?\\d+$") || (Integer.valueOf(strs[3]) != -1 && Integer.valueOf(strs[3]) != 1)) {
                appendErrorToExcel(helper, strs, "请输入正确的状态");
                continue;
            }
            String skuCode = strs[0].replace("\"", "");
            try {
                if(isYunJuJIT){
                    if(CollectionUtils.isEmpty(groupIds)){
                        appendErrorToExcel(helper, strs, "该skuCode不在该店铺的销售范围内");
                        continue;
                    }
                    params.put("skuCode",skuCode);
                    Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 50, TEMPLATE_NAME, params, SearchSkuTemplate.class);
                    if (!response.isSuccess()) {
                        log.error("query sku template by params:{}  fail,error:{}", params, response.getError());
                        appendErrorToExcel(helper, strs, response.getError());
                    }
                    if (response.getResult().getTotal() == 0) {
                        appendErrorToExcel(helper, strs, "该skuCode不在该店铺的销售范围内");
                        continue;
                    }
                }else{
                    Response<Optional<ItemMapping>> resp = mappingReadService.findBySkuCodeAndOpenShopId(skuCode, openShopId);
                    //不存在记录日志
                    if (!resp.isSuccess()) {
                        appendErrorToExcel(helper, strs, resp.getError());
                        continue;
                    }
                    if (!resp.getResult().isPresent()) {
                        appendErrorToExcel(helper, strs, "该skuCode在当前店铺内无映射关系");
                        continue;
                    }
                }
                ShopWarehouseSkuStockRule rule = new ShopWarehouseSkuStockRule();
                rule.setWarehouseId(warehouseId);
                rule.setShopId(openShopId);
                rule.setSkuCode(skuCode);
                rule.setSafeStock(Long.valueOf(strs[1]));
                rule.setRatio(Integer.valueOf(strs[2]));
                rule.setStatus(Integer.valueOf(strs[3]));
                rules.add(rule);

            } catch (Exception jre) {
                appendErrorToExcel(helper, strs, "处理失败");
                log.error("import make item group sku code:{} flag fail, cause:{}",
                        skuCode, Throwables.getStackTraceAsString(jre));
            }

        }
        List<ShopWarehouseSkuStockRule> pageList = Lists.newArrayList();
        if (!StringUtils.isEmpty(rules)) {
            try {
                for (int i = 0; i < rules.size(); i++) {
                    pageList.add(rules.get(i));
                    //分页处理
                    if (pageList.size() % DEFAULT_PAGE_SIZE == 0 || i == rules.size() - 1) {
                        Map<String, ShopWarehouseSkuStockRule> ruleMap = new HashMap<>();
                        for (ShopWarehouseSkuStockRule rule : pageList) {
                            ruleMap.put(rule.getSkuCode(), rule);
                        }
                        Response<List<String>> resp = warehouseShopSkuRuleClient.batchCreateOrUpdate(pageList);
                        //如果执行失败，则认为这一批都失败
                        if (!resp.isSuccess()) {
                            for (ShopWarehouseSkuStockRule rule : pageList) {
                                appendErrorToExcel(helper, createStrs(rule), resp.getError());
                            }

                        }
                        //如果有返回错误的skuCode
                        if (!StringUtils.isEmpty(resp.getResult())) {
                            for (String skuCode : resp.getResult()) {
                                appendErrorToExcel(helper, createStrs(ruleMap.get(skuCode)), "更新或创建异常");
                            }
                        }
                        pageList.clear();
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
            log.error("import warehouse shop sku rule abnormality");
        }
        return poushengCompensateBiz;
    }
}
