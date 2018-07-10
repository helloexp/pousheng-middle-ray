package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleWarehouseDao;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.group.service.ItemRuleWarehouseReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表读服务实现类
 * Date: 2018-07-13
 */
@Slf4j
@Service
public class ItemRuleWarehouseReadServiceImpl implements ItemRuleWarehouseReadService {

    @Autowired
    private ItemRuleWarehouseDao itemRuleWarehouseDao;

    @Autowired
    public ItemRuleWarehouseReadServiceImpl(ItemRuleWarehouseDao itemRuleWarehouseDao) {
        this.itemRuleWarehouseDao = itemRuleWarehouseDao;
    }

    @Override
    public Response<ItemRuleWarehouse> findById(Long Id) {
        try {
            return Response.ok(itemRuleWarehouseDao.findById(Id));
        } catch (Exception e) {
            log.error("find itemRuleWarehouse by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.find.fail");
        }
    }

    @Override
    public Response<List<ItemRuleWarehouse>> findByRuleId(Long ruleId) {
        try {
            return Response.ok(itemRuleWarehouseDao.findByRuleId(ruleId));
        } catch (Exception e) {
            log.error("find itemRuleWarehouse by rule id :{} failed,  cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.find.fail");
        }
    }

    @Override
    public Response<List<Long>> findWarehouseIds() {
        try {
            return Response.ok(itemRuleWarehouseDao.findWarehouseIds());
        } catch (Exception e) {
            log.error("find warehouse id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkWarehouseIds(Long ruleId, List<Long> warehouseIds) {
        try {
            return Response.ok(itemRuleWarehouseDao.checkWarehouseIds(ruleId,warehouseIds));
        } catch (Exception e) {
            log.error("find warehouse id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.find.fail");
        }
    }

    @Override
    public Response<Long> findRuleIdByWarehouseId(Long warehouseId) {
        try {
            return Response.ok(itemRuleWarehouseDao.findRuleIdByWarehouseId(warehouseId));
        } catch (Exception e) {
            log.error("find warehouse id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.find.fail");
        }
    }
}
