package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule")
@Slf4j
public class WarehouseRules {

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    @RpcConsumer
    private WarehouseRuleWriteService warehouseRuleWriteService;


    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RuleDto> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                      @RequestParam(required = false, value = "pageSize") Integer pageSize){

        Response<Paging<RuleDto>> r = warehouseRuleReadService.pagination(pageNo, pageSize);
        if(!r.isSuccess()){
            log.error("failed to pagination rule summary, error code:{}", r.getError());
            throw new JsonResponseException("warehouse.rule.find.fail");
        }
        return r.getResult();
    }

    @RequestMapping(value="/{ruleId}",method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long ruleId){
        Response<Boolean> r = warehouseRuleWriteService.deleteById(ruleId);
        if(!r.isSuccess()){
            log.error("failed to delete warehouse rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
}
