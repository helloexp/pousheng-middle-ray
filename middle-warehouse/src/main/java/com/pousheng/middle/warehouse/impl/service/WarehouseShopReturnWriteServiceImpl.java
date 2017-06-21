package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopReturnDao;
import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import com.pousheng.middle.warehouse.service.WarehouseShopReturnWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库写服务实现类
 * Date: 2017-06-21
 */
@Slf4j
@Service
public class WarehouseShopReturnWriteServiceImpl implements WarehouseShopReturnWriteService {

    private final WarehouseShopReturnDao warehouseShopReturnDao;

    @Autowired
    public WarehouseShopReturnWriteServiceImpl(WarehouseShopReturnDao warehouseShopReturnDao) {
        this.warehouseShopReturnDao = warehouseShopReturnDao;
    }

    @Override
    public Response<Long> create(WarehouseShopReturn warehouseShopReturn) {
        try {
            Long shopId = warehouseShopReturn.getShopId();
            WarehouseShopReturn exist = warehouseShopReturnDao.findByShopId(shopId);
            if(exist!=null){
                log.error("failed to create {}, because warehouse for shop(id={}) has existed",
                        warehouseShopReturn, shopId);
                return Response.fail("shopId.duplicated");
            }
            warehouseShopReturnDao.create(warehouseShopReturn);
            return Response.ok(warehouseShopReturn.getId());
        } catch (Exception e) {
            log.error("create warehouseShopReturn failed, warehouseShopReturn:{}, cause:{}", warehouseShopReturn, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseShopReturn warehouseShopReturn) {
        try {
            return Response.ok(warehouseShopReturnDao.update(warehouseShopReturn));
        } catch (Exception e) {
            log.error("update warehouseShopReturn failed, warehouseShopReturn:{}, cause:{}", warehouseShopReturn, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseShopReturnId) {
        try {
            return Response.ok(warehouseShopReturnDao.delete(warehouseShopReturnId));
        } catch (Exception e) {
            log.error("delete warehouseShopReturn failed, warehouseShopReturnId:{}, cause:{}", warehouseShopReturnId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.delete.fail");
        }
    }
}
