package com.pousheng.middle.web.excel.supplyRule.parser;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.ShopSkuSupplyRule;
import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleDTO;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import io.terminus.common.model.Response;
import io.terminus.excel.read.handler.AbstractExecuteHandler;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:31<br/>
 */
@Slf4j
public class SupplyRuleExecuteHandler extends AbstractExecuteHandler<SupplyRuleDTO> {
    private final ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;
    private final ImportProgressStatus importProgressStatus;
    private final Boolean delta;

    public SupplyRuleExecuteHandler(Boolean delta, ImportProgressStatus progressStatus, ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent) {
        this.delta = delta;
        importProgressStatus = progressStatus;
        this.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
    }

    @Override
    public void batchExecute(List<SupplyRuleDTO> list) {
        for (SupplyRuleDTO rule : list) {
            try {
                doSave(rule);
            } catch (Exception e) {
                log.error("failed to save supply rule: {}, cause: {}", rule, Throwables.getStackTraceAsString(e));
                importProgressStatus.fail(rule.getSource(), "保存时失败，" + e.getMessage(), rule.getRowNo());
            }
        }
    }

    private void doSave(SupplyRuleDTO rule) {
        if (CollectionUtils.isEmpty(rule.getSkuTemplate())) {
            return;
        }
        for (SkuTemplate skuTemplate : rule.getSkuTemplate()) {
            if (delta) {
                Response<ShopSkuSupplyRule> r = shopSkuSupplyRuleComponent.queryByShopIdAndSkuCode(rule.getShop().getOpenShopId(), skuTemplate.getSkuCode(), rule.getStatus());
                if (!r.isSuccess()) {
                    log.error("failed to find shop sku supply rule:{}, cause: {}", rule, r.getError());
                    importProgressStatus.fail(rule.getSource(), "校验是否已存在的规则失败，" + r.getError(), rule.getRowNo());
                    return;
                }
                ShopSkuSupplyRule exist = r.getResult();
                if (exist != null && !Objects.equals(exist.getType(), rule.getType())) {
                    importProgressStatus.fail(rule.getSource(), "当前规则存在，且为“" + exist.getType() + "”与导入文件规则违背，无法增量", rule.getRowNo());
                    return;
                }
            }

            Response<Boolean> response = shopSkuSupplyRuleComponent.save(rule.getShop(), skuTemplate, rule.getType(), rule.getWarehouseCodes(), rule.getStatus(), delta);
            if (!response.isSuccess() || !response.getResult()) {
                log.error("fail to update shop sku supply rule, shop:{}, sku: {}, type: {}, warehouse: {}, status: {},cause:{}",
                        rule.getShop(), skuTemplate, rule.getType(), rule.getWarehouseCodes(), rule.getStatus(),
                        response.getError());

                importProgressStatus.fail(rule.getSource(), "保存失败", rule.getRowNo());
                return;
            }
        }
        importProgressStatus.success();
    }
}
