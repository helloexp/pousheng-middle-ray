package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-21
 */
@RestController
@RequestMapping("/api/warehouse/company-rule")
@Slf4j
public class WarehouseCompanyRules {

    @RpcConsumer
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;

    @RpcConsumer
    private WarehouseCompanyRuleWriteService warehouseCompanyRuleWriteService;


    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseCompanyRule warehouseCompanyRule){
        Response<Long> r = warehouseCompanyRuleWriteService.create(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody WarehouseCompanyRule warehouseCompanyRule){
        Response<Boolean> r = warehouseCompanyRuleWriteService.update(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long id){
        Response<Boolean> r = warehouseCompanyRuleWriteService.deleteById(id);
        if(!r.isSuccess()){
            log.error("failed to delete WarehouseCompanyRule(id={}_, error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/paging",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseCompanyRule> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                   @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                   @RequestParam(required = false, value="companyCode")String companyCode){
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);
        if(StringUtils.hasText(companyCode)){
            params.put("companyCode", companyCode);
        }
        Response<Paging<WarehouseCompanyRule>> r = warehouseCompanyRuleReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination WarehouseCompanyRule, params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


}
