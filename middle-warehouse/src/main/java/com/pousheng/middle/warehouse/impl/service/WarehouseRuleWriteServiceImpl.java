package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.manager.WarehouseRuleManager;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.service.WarehouseRuleWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleWriteServiceImpl implements WarehouseRuleWriteService {

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseRuleManager ruleManager;

    @Autowired
    public WarehouseRuleWriteServiceImpl(WarehouseRuleDao warehouseRuleDao, WarehouseRuleManager ruleManager) {
        this.warehouseRuleDao = warehouseRuleDao;
        this.ruleManager = ruleManager;
    }

    @Override
    public Response<Long> create(WarehouseRule warehouseRule) {
        try {
            warehouseRuleDao.create(warehouseRule);
            return Response.ok(warehouseRule.getId());
        } catch (Exception e) {
            log.error("batchCreate warehouseRule failed, warehouseRule:{}, cause:{}", warehouseRule, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.batchCreate.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseRule warehouseRule) {
        try {
            return Response.ok(warehouseRuleDao.update(warehouseRule));
        } catch (Exception e) {
            log.error("update warehouseRule failed, warehouseRule:{}, cause:{}", warehouseRule, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseRuleId) {
        try {
            ruleManager.delete(warehouseRuleId);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete warehouseRule failed, warehouseRuleId:{}, cause:{}", warehouseRuleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.delete.fail");
        }
    }
}
