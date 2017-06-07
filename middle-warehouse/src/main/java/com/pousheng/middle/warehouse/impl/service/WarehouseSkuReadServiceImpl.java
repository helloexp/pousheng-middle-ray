package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuDao;
import com.pousheng.middle.warehouse.model.WarehouseSku;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseSkuReadServiceImpl implements WarehouseSkuReadService {

    private final WarehouseSkuDao warehouseSkuDao;

    @Autowired
    public WarehouseSkuReadServiceImpl(WarehouseSkuDao warehouseSkuDao) {
        this.warehouseSkuDao = warehouseSkuDao;
    }

    @Override
    public Response<WarehouseSku> findById(Long Id) {
        try {
            return Response.ok(warehouseSkuDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseSku by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }
}
