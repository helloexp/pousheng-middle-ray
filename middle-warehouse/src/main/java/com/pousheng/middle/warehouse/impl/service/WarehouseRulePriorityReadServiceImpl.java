package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.RulePriorityCriteria;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRulePriorityDao;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author: zhaoxiaowei
 * Desc: 读服务实现类
 * Date: 2018-09-04
 */
@Slf4j
@Service
@RpcProvider
public class WarehouseRulePriorityReadServiceImpl implements WarehouseRulePriorityReadService {

    @Autowired
    private WarehouseRulePriorityDao warehouseRulePriorityDao;

    @Override
    public Response<WarehouseRulePriority> findById(Long id) {
        try {
            WarehouseRulePriority warehouseRulePriority = warehouseRulePriorityDao.findById(id);
            if (warehouseRulePriority == null) {
                return Response.fail("warehouse.rule.priority.not.exist");
            }
            return Response.ok(warehouseRulePriority);
        } catch (Exception e) {
            log.error("find warehouseRulePriority by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.find.fail");
        }
    }

    @Override
    public Response<Paging<WarehouseRulePriority>> findByCriteria(RulePriorityCriteria criteria) {
        try {
            Paging<WarehouseRulePriority> paging = warehouseRulePriorityDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging warehouse rule priority, criteria={}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkByName(WarehouseRulePriority warehouseRulePriority) {
        try {
            return Response.ok(warehouseRulePriorityDao.checkByName(warehouseRulePriority));
        } catch (Exception e) {
            log.error("failed to check name data:{} cause:{}", warehouseRulePriority, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.name.duplicate");
        }
    }

    @Override
    public Response<Boolean> checkTimeRange(WarehouseRulePriority warehouseRulePriority) {
        try {
            return Response.ok(warehouseRulePriorityDao.checkTimeRange(warehouseRulePriority));
        } catch (Exception e) {
            log.error("failed to check name data:{} cause:{}", warehouseRulePriority, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.priority.range.coincidence");
        }
    }
}
