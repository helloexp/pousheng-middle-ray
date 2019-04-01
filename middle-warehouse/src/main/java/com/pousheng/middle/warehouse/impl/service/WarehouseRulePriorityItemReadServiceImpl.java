package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRulePriorityItemDao;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: zhaoxiaowei
 * Desc: 读服务实现类
 * Date: 2018-09-04
 */
@Slf4j
@Service
@RpcProvider
public class WarehouseRulePriorityItemReadServiceImpl implements WarehouseRulePriorityItemReadService {


    @Autowired
    private WarehouseRulePriorityItemDao warehouseRulePriorityItemDao;

    @Override
    public Response<WarehouseRulePriorityItem> findById(Long id) {
        try {
            return Response.ok(warehouseRulePriorityItemDao.findById(id));
        } catch (Exception e) {
            log.error("find warehouseRulePriorityItem by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.find.fail");
        }
    }

    @Override
    public Response<WarehouseRulePriorityItem> findByEntity(WarehouseRulePriorityItem item) {
        try {
            return Response.ok(warehouseRulePriorityItemDao.findByEntity(item));
        } catch (Exception e) {
            log.error("find warehouseRulePriorityItem by item :{} failed,  cause:{}", item, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.find.fail");

        }
    }

    @Override
    public Response<List<WarehouseRulePriorityItem>> findByPriorityId(Long priorityId) {
        try {
            return Response.ok(warehouseRulePriorityItemDao.findByPriorityId(priorityId));
        } catch (Exception e) {
            log.error("find warehouseRulePriorityItem by priorityId :{} failed,  cause:{}", priorityId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.item.find.fail");

        }
    }
}
