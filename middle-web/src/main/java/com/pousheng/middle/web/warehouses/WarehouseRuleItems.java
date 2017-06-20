package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule/{ruleId}/rule-item")
@Slf4j
public class WarehouseRuleItems {

    @RpcConsumer
    private WarehouseRuleItemReadService warehouseRuleItemReadService;

    @RpcConsumer
    private WarehouseRuleItemWriteService warehouseRuleItemWriteService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WarehouseRuleItem> findByRuleId(@PathVariable Long ruleId){
        Response<List<WarehouseRuleItem>> r = warehouseRuleItemReadService.findByRuleId(ruleId);
        if(!r.isSuccess()){
            log.error("failed to find warehouse rule items for rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean save(@PathVariable Long ruleId, @RequestBody WarehouseRuleItem[] warehouseRuleItems){
        ArrayList<WarehouseRuleItem> ruleItemArrayList = Lists.newArrayList(warehouseRuleItems);
        Response<Boolean> r = warehouseRuleItemWriteService.batchCreate(ruleId, ruleItemArrayList);
        if(!r.isSuccess()){
            log.error("failed to save rule(id={})'s warehouseRuleItems:{}, error code:{}",
                    ruleId, warehouseRuleItems,r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
}
