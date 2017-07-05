package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.manager.WarehouseShopRuleManager;
import com.pousheng.middle.warehouse.service.WarehouseShopRuleWriteService;
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
public class WarehouseShopRuleWriteServiceImpl implements WarehouseShopRuleWriteService {

    private final WarehouseShopRuleManager warehouseShopRuleManager;

    @Autowired
    public WarehouseShopRuleWriteServiceImpl(WarehouseShopRuleManager warehouseShopRuleManager) {
        this.warehouseShopRuleManager = warehouseShopRuleManager;
    }

    /**
     * 创建店铺到发货规则的映射关系
     *
     * @param thinShops 店铺列表
     * @return 对应的规则id
     */
    @Override
    public Response<Long> batchCreate(List<ThinShop> thinShops) {
        try {
            Long rid = warehouseShopRuleManager.batchCreate(thinShops);
            return Response.ok(rid);
        } catch (Exception e) {
            log.error("failed to batchCreate warehouseShopRule with shops:{}, cause:{}",
                    thinShops,  Throwables.getStackTraceAsString(e));
            return Response.fail("shop.may.conflict");
        }
    }

    /**
     * 更新规则对应的店铺列表
     *
     * @param ruleId 规则id
     * @param shops  店铺列表
     * @return 对应的规则id
     */
    @Override
    public Response<Boolean> batchUpdate(Long ruleId, List<ThinShop> shops) {
        try {
            warehouseShopRuleManager.batchUpdate(ruleId, shops);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to update warehouseShopRule(ruleId={}) with shops:{}, cause:{}",
                    ruleId, shops, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.may.conflict");
        }
    }

    /**
     * 根据规则id删除店铺到发货规则的映射关系
     *
     * @param ruleId 规则id
     * @return 是否成功
     */
    @Override
    public Response<Boolean> deleteByRuleId(Long ruleId) {
        try {
            warehouseShopRuleManager.deleteByRuleId(ruleId);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete warehouseAddressRule failed, ruleId={}, cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.rule.delete.fail");
        }
    }
}
