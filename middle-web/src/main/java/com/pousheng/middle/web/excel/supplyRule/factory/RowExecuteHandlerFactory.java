package com.pousheng.middle.web.excel.supplyRule.factory;

import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.parser.SupplyRuleExecuteHandler;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-11 11:41<br/>
 */
@Component
public class RowExecuteHandlerFactory {
    private static ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;

    public RowExecuteHandlerFactory(ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent) {
        RowExecuteHandlerFactory.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
    }

    public static SupplyRuleExecuteHandler get(Boolean delta, ImportProgressStatus progressStatus) {
        SupplyRuleExecuteHandler supplyRuleExecuteHandler = new SupplyRuleExecuteHandler(delta, progressStatus, shopSkuSupplyRuleComponent);
        supplyRuleExecuteHandler.setBatchSize(5);
        return supplyRuleExecuteHandler;
    }
}
