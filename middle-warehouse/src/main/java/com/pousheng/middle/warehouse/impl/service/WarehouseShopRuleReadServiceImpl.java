package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseShopRule;
import com.pousheng.middle.warehouse.service.WarehouseShopRuleReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Service
@Slf4j
public class WarehouseShopRuleReadServiceImpl implements WarehouseShopRuleReadService {

    private final  WarehouseShopRuleDao warehouseShopRuleDao;

    @Autowired
    public WarehouseShopRuleReadServiceImpl(WarehouseShopRuleDao warehouseShopRuleDao) {
        this.warehouseShopRuleDao = warehouseShopRuleDao;
    }

    /**
     * 根据规则找对应的店铺列表
     *
     * @param ruleId 规则id
     * @return 对应的店铺列表
     */
    @Override
    public Response<List<WarehouseShopRule>> findByRuleId(Long ruleId) {
        try {
            return Response.ok(warehouseShopRuleDao.findByRuleId(ruleId));
        } catch (Exception e) {
            log.error("failed to find warehouseShopRule by ruleId={}, cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shopRule.find.fail");
        }
    }
}
