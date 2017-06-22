package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import com.pousheng.middle.warehouse.service.WarehouseShopReturnReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopReturnWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-21
 */
@RestController
@RequestMapping("/api/warehouse/shop-return")
@Slf4j
public class WarehouseShopReturns {

    @RpcConsumer
    private WarehouseShopReturnReadService warehouseShopReturnReadService;

    @RpcConsumer
    private WarehouseShopReturnWriteService warehouseShopReturnWriteService;


    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseShopReturn warehouseShopReturn){
        Response<Long> r = warehouseShopReturnWriteService.create(warehouseShopReturn);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouseShopReturn, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody WarehouseShopReturn warehouseShopReturn){
        Response<Boolean> r = warehouseShopReturnWriteService.update(warehouseShopReturn);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouseShopReturn, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long id){
        Response<Boolean> r = warehouseShopReturnWriteService.deleteById(id);
        if(!r.isSuccess()){
            log.error("failed to delete WarehouseShopReturn(id={}_, error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/shop/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseShopReturn findByShopId(@RequestParam Long shopId){
        Response<WarehouseShopReturn> r = warehouseShopReturnReadService.findByShopId(shopId);
        if(!r.isSuccess()){
            log.error("failed to delete WarehouseShopReturn(shop_id={}, error code:{}", shopId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/paging",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseShopReturn> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                  @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                  @RequestParam(required = false, value="shopId")Long shopId){
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);
        if(shopId!=null){
            params.put("shopId", shopId);
        }
        Response<Paging<WarehouseShopReturn>> r = warehouseShopReturnReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination WarehouseShopReturn, params, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


}
