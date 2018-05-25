package com.pousheng.middle.warehouse.companent;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.WarehouseRuleDto;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 派单规则API调用
 *
 * @auther feisheng.ch
 * @time 2018/5/23
 */
@Component
@Slf4j
public class WarehouseRulesItemClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 根据ID获取规则
     * @param ruleId
     * @return
     */
    public Response<WarehouseRuleDto> findByRuleId (Long ruleId) {
        try {
            return Response.ok(
                    (WarehouseRuleDto)
                            inventoryBaseClient.get("/api/inventory/rule/"+ruleId+"/rule-item",
                    null, null, Maps.newHashMap(), WarehouseRuleDto.class, false)
            );
        } catch (Exception e) {
            log.error("find rule item by id fail, ruleId:{}, cause:{}", ruleId, Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

}
