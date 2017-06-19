package com.pousheng.middle.warehouse.cache;

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
    private final ConcurrentMap<Long, Warehouse> cache;

    private final WarehouseReadService warehouseReadService;

    @Autowired
    public WarehouseCacher(WarehouseReadService warehouseReadService) {
        this.warehouseReadService = warehouseReadService;
        cache = new ConcurrentHashMap<Long, Warehouse>();
    }

    public Warehouse findById(Long id){
        return cache.computeIfAbsent(id, new Function<Long, Warehouse>() {
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
}
