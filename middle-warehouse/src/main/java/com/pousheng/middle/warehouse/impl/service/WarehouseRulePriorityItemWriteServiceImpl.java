package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRulePriorityItemDao;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: zhaoxiaowei
 * Desc: 写服务实现类
 * Date: 2018-09-04
 */
@Slf4j
@Service
@RpcProvider
public class WarehouseRulePriorityItemWriteServiceImpl implements WarehouseRulePriorityItemWriteService {

    @Autowired
    private WarehouseRulePriorityItemDao warehouseRulePriorityItemDao;

    @Override
    public Response<Long> create(WarehouseRulePriorityItem warehouseRulePriorityItem) {
        try {
            warehouseRulePriorityItemDao.create(warehouseRulePriorityItem);
            return Response.ok(warehouseRulePriorityItem.getId());
        } catch (Exception e) {
            log.error("create warehouseRulePriorityItem failed, warehouseRulePriorityItem:{}, cause:{}", warehouseRulePriorityItem, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseRulePriorityItem warehouseRulePriorityItem) {
        try {
            return Response.ok(warehouseRulePriorityItemDao.update(warehouseRulePriorityItem));
        } catch (Exception e) {
            log.error("update warehouseRulePriorityItem failed, warehouseRulePriorityItem:{}, cause:{}", warehouseRulePriorityItem, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseRulePriorityItemId) {
        try {
            return Response.ok(warehouseRulePriorityItemDao.delete(warehouseRulePriorityItemId));
        } catch (Exception e) {
            log.error("delete warehouseRulePriorityItem failed, warehouseRulePriorityItemId:{}, cause:{}", warehouseRulePriorityItemId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.delete.fail");
        }
    }
}
