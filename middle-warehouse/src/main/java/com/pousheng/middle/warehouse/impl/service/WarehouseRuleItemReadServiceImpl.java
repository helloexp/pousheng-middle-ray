package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则项读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleItemReadServiceImpl implements WarehouseRuleItemReadService {

    private final WarehouseRuleItemDao warehouseRuleItemDao;

    @Autowired
    public WarehouseRuleItemReadServiceImpl(WarehouseRuleItemDao warehouseRuleItemDao) {
        this.warehouseRuleItemDao = warehouseRuleItemDao;
    }

    @Override
    public Response<WarehouseRuleItem> findById(Long Id) {
        try {
            return Response.ok(warehouseRuleItemDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseRuleItem by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.find.fail");
        }
    }
}
