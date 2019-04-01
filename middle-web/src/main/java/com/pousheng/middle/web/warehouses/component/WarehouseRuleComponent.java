package com.pousheng.middle.web.warehouses.component;

import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2018/2/20
 */
@Component
@Slf4j
public class WarehouseRuleComponent {

    @Autowired
    private WarehouseRulesClient warehouseRulesClient;

    public List<WarehouseShopGroup> findWarehouseShopGropsByRuleId(Long ruleId){
        WarehouseRule warehouseRule = warehouseRulesClient.findRuleById(ruleId);
        if (null == warehouseRule){
            log.error("failed to query warehouse rule (ruleId={})", ruleId);
            throw new JsonResponseException("warehouse.rule.find.fail");
        }
        Long shopGroupId = warehouseRule.getShopGroupId();
        List<WarehouseShopGroup> rwsrs = warehouseRulesClient.findShopGroupById(shopGroupId);
        if (null == rwsrs) {
            log.error("failed to find warehouseShopGroups by shopGroupId={}", shopGroupId);
            throw new JsonResponseException("warehouse.rule.shop.find.fail");
        }
        return rwsrs;
    }
}
