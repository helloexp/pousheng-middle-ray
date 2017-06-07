package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleReadServiceImpl implements WarehouseRuleReadService {

    private final WarehouseRuleDao warehouseRuleDao;

    @Autowired
    public WarehouseRuleReadServiceImpl(WarehouseRuleDao warehouseRuleDao) {
        this.warehouseRuleDao = warehouseRuleDao;
    }

    @Override
    public Response<WarehouseRule> findById(Long Id) {
        try {
            return Response.ok(warehouseRuleDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseRule by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.find.fail");
        }
    }
}
