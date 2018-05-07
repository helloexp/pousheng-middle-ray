package com.pousheng.middle.warehouse.cache;

import com.google.common.base.Optional;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-19
 */
@Component
@Slf4j
public class WarehouseCacher {
    private final ConcurrentMap<Long, Warehouse> byId;

    private final ConcurrentMap<String, Warehouse> byCode;

    private final WarehouseReadService warehouseReadService;

    @Autowired
    public WarehouseCacher(WarehouseReadService warehouseReadService) {
        this.warehouseReadService = warehouseReadService;
        byId = new ConcurrentHashMap<>();
        byCode = new ConcurrentHashMap<>();
    }

    public Warehouse findById(Long id){
        return byId.computeIfAbsent(id, new Function<Long, Warehouse>() {
            @Override
            public Warehouse apply(Long id) {
                Response<Warehouse> r =  warehouseReadService.findById(id);
                if(!r.isSuccess()){
                    log.error("failed to find warehouse(id={}), error code:{}", id, r.getError());
                    throw new ServiceException(r.getError());
                }
                return r.getResult();
            }
        });
    }

    public Warehouse findByCode(String code){
        return byCode.computeIfAbsent(code, new Function<String, Warehouse>() {
            @Override
            public Warehouse apply(String code) {
                Response<Optional<Warehouse>> r =  warehouseReadService.findByCode(code);
                if(!r.isSuccess()){
                    log.error("failed to find warehouse(code={}), error code:{}", code, r.getError());
                    throw new ServiceException(r.getError());
                }
                Optional<Warehouse> result = r.getResult();
                if(result.isPresent()) {
                    return result.get();
                }else{
                    log.error("warehouse(code={}) not found", code);
                    throw new ServiceException("warehouse not found");
                }
            }
        });
    }


    public void refreshById(Long id){

        Response<Warehouse> r =  warehouseReadService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find warehouse(id={}), error code:{}", id, r.getError());
            throw new ServiceException(r.getError());
        }
        Warehouse exist = r.getResult();

        if(byId.containsKey(id)){
            byId.replace(id,exist);
        }else {
            byId.putIfAbsent(id,exist);
        }

        if(byCode.containsKey(exist.getCode())){
            byCode.replace(exist.getCode(),exist);
        }else {
            byCode.putIfAbsent(exist.getCode(),exist);
        }
    }



}
