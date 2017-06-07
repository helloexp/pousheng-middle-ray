package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseDao;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseReadServiceImpl implements WarehouseReadService {

    private final WarehouseDao warehouseDao;

    @Autowired
    public WarehouseReadServiceImpl(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    @Override
    public Response<Warehouse> findById(Long Id) {
        try {
            return Response.ok(warehouseDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouse by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }
}
