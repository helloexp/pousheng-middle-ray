package com.pousheng.middle.web.async.supplyRule;

import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import io.terminus.common.model.Response;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/6
 */
@NoArgsConstructor
@Slf4j
public class SkuSupplyRuleDisableHandle implements Callable<Long> {
    public static final int LIMIT_SIZE =  2000;

    private List<String> skuCodes = new ArrayList<>(LIMIT_SIZE);

    private Long shopId;

    private Long upperLimitId;

    private ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;

    public SkuSupplyRuleDisableHandle(Long shopId, Long upperLimitId, ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent) {
        this.shopId = shopId;
        this.upperLimitId = upperLimitId;
        this.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
    }

    public boolean isFull() {
        return skuCodes.size() >= LIMIT_SIZE;
    }

    public void append(String skuCode) {
        skuCodes.add(skuCode);
    }

    public Integer size() {
        return skuCodes.size();
    }

    @Override
    public Long call() {
//        log.info("[sku-supply-rule-disable-handle] sku size:{}", skuCodes.size());
        Response<Long> response = shopSkuSupplyRuleComponent.batchUpdateDisable(shopId, skuCodes, upperLimitId);
//        log.info("[sku-supply-rule-disable-handle]  processed size:{}", response.getResult());
        return response.getResult();
    }
}
