package com.pousheng.middle.warehouses;

import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@RestController
@RequestMapping("/api/warehouse")
@Slf4j
public class Warehouses {

    @RpcConsumer
    private WarehouseWriteService warehouseWriteService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody  Warehouse warehouse){
        Response<Long> r =  warehouseWriteService.create(warehouse);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody Warehouse warehouse){
        Response<Boolean> r =  warehouseWriteService.update(warehouse);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean delete(@PathVariable("id")Long id){
        Response<Boolean>  r = warehouseWriteService.deleteById(id);
        if(!r.isSuccess()){
            log.error("failed to delete warehouse(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }

        return true;
    }


}
