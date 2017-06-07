package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleItemWriteServiceImpl implements WarehouseRuleItemWriteService {

    private final WarehouseRuleItemDao warehouseRuleItemDao;

    @Autowired
    public WarehouseRuleItemWriteServiceImpl(WarehouseRuleItemDao warehouseRuleItemDao) {
        this.warehouseRuleItemDao = warehouseRuleItemDao;
    }

    @Override
    public Response<Long> create(WarehouseRuleItem warehouseRuleItem) {
        try {
            warehouseRuleItemDao.create(warehouseRuleItem);
            return Response.ok(warehouseRuleItem.getId());
        } catch (Exception e) {
            log.error("create warehouseRuleItem failed, warehouseRuleItem:{}, cause:{}", warehouseRuleItem, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseRuleItem warehouseRuleItem) {
        try {
            return Response.ok(warehouseRuleItemDao.update(warehouseRuleItem));
        } catch (Exception e) {
            log.error("update warehouseRuleItem failed, warehouseRuleItem:{}, cause:{}", warehouseRuleItem, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseRuleItemId) {
        try {
            return Response.ok(warehouseRuleItemDao.delete(warehouseRuleItemId));
        } catch (Exception e) {
            log.error("delete warehouseRuleItem failed, warehouseRuleItemId:{}, cause:{}", warehouseRuleItemId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.delete.fail");
        }
    }
}
