package com.pousheng.middle.web.warehouses.component;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopGroupReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2018/2/20
 */
@Component
@Slf4j
public class WarehouseRuleComponent {

    @RpcConsumer
    private WarehouseShopGroupReadService warehouseShopGroupReadService;
    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    public List<WarehouseShopGroup> findWarehouseShopGropsByRuleId(Long ruleId){
        Response<WarehouseRule> r = warehouseRuleReadService.findById(ruleId);
        if (!r.isSuccess()){
            log.error("failed to query warehouse rule (ruleId={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        WarehouseRule warehouseRule = r.getResult();
        Long shopGroupId = warehouseRule.getShopGroupId();
        Response<List<WarehouseShopGroup>> rwsrs = warehouseShopGroupReadService.findByGroupId(shopGroupId);
        if (!rwsrs.isSuccess()) {
            log.error("failed to find warehouseShopGroups by shopGroupId={}, error code:{}", shopGroupId, rwsrs.getError());
            throw new JsonResponseException(rwsrs.getError());
        }
        return rwsrs.getResult();
    }
}
