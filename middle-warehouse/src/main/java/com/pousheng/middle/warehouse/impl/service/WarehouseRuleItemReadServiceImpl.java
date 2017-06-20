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
    public Response<WarehouseRuleItem> findById(Long id) {
        try {
            return Response.ok(warehouseRuleItemDao.findById(id));
        } catch (Exception e) {
            log.error("find warehouseRuleItem by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.find.fail");
        }
    }

    /**
     * 根据规则id查找关联的仓库
     *
     * @param ruleId 规则id
     * @return 规则关联的仓库
     */
    @Override
    public Response<List<WarehouseRuleItem>> findByRuleId(Long ruleId) {
        try {
            return Response.ok(warehouseRuleItemDao.findByRuleId(ruleId));
        } catch (Exception e) {
            log.error("find warehouseRuleItem by ruleId :{} failed,  cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.item.find.fail");
        }
    }
}
