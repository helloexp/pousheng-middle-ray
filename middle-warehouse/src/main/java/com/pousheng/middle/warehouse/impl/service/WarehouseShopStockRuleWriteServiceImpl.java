package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopStockRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
@Slf4j
@Service
public class WarehouseShopStockRuleWriteServiceImpl implements WarehouseShopStockRuleWriteService {

    @Autowired
    private WarehouseShopStockRuleDao warehouseShopStockRuleDao;

    @Override
    public Response<Long> create(WarehouseShopStockRule warehouseShopStockRule) {
        try{
            Long shopId = warehouseShopStockRule.getShopId();
            WarehouseShopStockRule  exist = warehouseShopStockRuleDao.findByShopId(shopId);
            if(exist!=null){
                log.error("warehouse shop stock rule(shopId={}) has exist", shopId);
                return Response.fail("shop.rule.duplicated");
            }
            warehouseShopStockRule.setStatus(1);
            warehouseShopStockRuleDao.create(warehouseShopStockRule);
            return Response.ok(warehouseShopStockRule.getId());
        }catch (Exception e){
            log.error("failed to create warehouse shop stock rule, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.stock.rule.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseShopStockRule warehouseShopStockRule) {
        try{
            return Response.ok(warehouseShopStockRuleDao.update(warehouseShopStockRule));
        }catch (Exception e){
            log.error("failed to update warehouse shop stock rule, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.stock.rule.update.fail");
        }
    }

   @Override
    public Response<Boolean> delete(Long id) {
        try{
            return Response.ok(warehouseShopStockRuleDao.delete(id));
        }catch (Exception e){
            log.error("failed to delete warehouse shop stock rule by id:{}, cause:{}", id,  Throwables.getStackTraceAsString(e));
            return Response.fail("delete.warehouse.shop.stock.rule.fail");
        }
    }

}