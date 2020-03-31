package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleWarehouseDao;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.group.service.ItemRuleWarehouseWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表写服务实现类
 * Date: 2018-07-13
 */
@Slf4j
@Service
public class ItemRuleWarehouseWriteServiceImpl implements ItemRuleWarehouseWriteService {

    @Autowired
    private ItemRuleWarehouseDao itemRuleWarehouseDao;

    @Autowired
    public ItemRuleWarehouseWriteServiceImpl(ItemRuleWarehouseDao itemRuleWarehouseDao) {
        this.itemRuleWarehouseDao = itemRuleWarehouseDao;
    }

    @Override
    public Response<Long> create(ItemRuleWarehouse itemRuleWarehouse) {
        try {
            itemRuleWarehouseDao.create(itemRuleWarehouse);
            return Response.ok(itemRuleWarehouse.getId());
        } catch (Exception e) {
            log.error("create itemRuleWarehouse failed, itemRuleWarehouse:{}, cause:{}", itemRuleWarehouse, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemRuleWarehouse itemRuleWarehouse) {
        try {
            return Response.ok(itemRuleWarehouseDao.update(itemRuleWarehouse));
        } catch (Exception e) {
            log.error("update itemRuleWarehouse failed, itemRuleWarehouse:{}, cause:{}", itemRuleWarehouse, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long itemRuleWarehouseId) {
        try {
            return Response.ok(itemRuleWarehouseDao.delete(itemRuleWarehouseId));
        } catch (Exception e) {
            log.error("delete itemRuleWarehouse failed, itemRuleWarehouseId:{}, cause:{}", itemRuleWarehouseId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.warehouse.delete.fail");
        }
    }
}
