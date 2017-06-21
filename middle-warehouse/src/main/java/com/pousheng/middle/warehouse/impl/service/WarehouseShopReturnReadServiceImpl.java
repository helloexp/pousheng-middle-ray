package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopReturnDao;
import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import com.pousheng.middle.warehouse.service.WarehouseShopReturnReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库读服务实现类
 * Date: 2017-06-21
 */
@Slf4j
@Service
public class WarehouseShopReturnReadServiceImpl implements WarehouseShopReturnReadService {

    private final WarehouseShopReturnDao warehouseShopReturnDao;

    @Autowired
    public WarehouseShopReturnReadServiceImpl(WarehouseShopReturnDao warehouseShopReturnDao) {
        this.warehouseShopReturnDao = warehouseShopReturnDao;
    }

    @Override
    public Response<WarehouseShopReturn> findById(Long Id) {
        try {
            return Response.ok(warehouseShopReturnDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseShopReturn by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.find.fail");
        }
    }
}
