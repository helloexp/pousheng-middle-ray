package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRulePriorityDao;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityWriteService;
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
public class WarehouseRulePriorityWriteServiceImpl implements WarehouseRulePriorityWriteService {

    @Autowired
    private WarehouseRulePriorityDao warehouseRulePriorityDao;

    @Override
    public Response<Long> create(WarehouseRulePriority warehouseRulePriority) {
        try {
            warehouseRulePriorityDao.create(warehouseRulePriority);
            return Response.ok(warehouseRulePriority.getId());
        } catch (Exception e) {
            log.error("create warehouseRulePriority failed, warehouseRulePriority:{}, cause:{}", warehouseRulePriority, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseRulePriority warehouseRulePriority) {
        try {
            return Response.ok(warehouseRulePriorityDao.update(warehouseRulePriority));
        } catch (Exception e) {
            log.error("update warehouseRulePriority failed, warehouseRulePriority:{}, cause:{}", warehouseRulePriority, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseRulePriorityId) {
        try {
            return Response.ok(warehouseRulePriorityDao.delete(warehouseRulePriorityId));
        } catch (Exception e) {
            log.error("delete warehouseRulePriority failed, warehouseRulePriorityId:{}, cause:{}", warehouseRulePriorityId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.delete.fail");
        }
    }
}
