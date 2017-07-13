package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.manager.WarehouseAddressRuleManager;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseAddressRuleWriteServiceImpl implements WarehouseAddressRuleWriteService {

    private final WarehouseAddressRuleManager warehouseAddressRuleManager;

    private final WarehouseRuleDao warehouseRuleDao;

    @Autowired
    public WarehouseAddressRuleWriteServiceImpl(WarehouseAddressRuleManager warehouseAddressRuleManager,
                                                WarehouseRuleDao warehouseRuleDao) {
        this.warehouseAddressRuleManager = warehouseAddressRuleManager;
        this.warehouseRuleDao = warehouseRuleDao;
    }

    /**
     * 创建WarehouseAddresses
     *
     * @param  shopGroupId 店铺组id
     * @param thinAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    @Override
    public Response<Long> batchCreate(Long shopGroupId, List<ThinAddress> thinAddresses) {
        try {
            Long rid = warehouseAddressRuleManager.batchCreate(shopGroupId, thinAddresses);
            return Response.ok(rid);
        } catch (Exception e) {
            log.error("failed to batchCreate warehouseAddressRule with address:{} for shopGroup(id={}), cause:{}",
                    thinAddresses, shopGroupId, Throwables.getStackTraceAsString(e));
            return Response.fail("address.may.conflict");
        }
    }

    /**
     * 更新规则对应的warehouseAddresses
     *
     * @param ruleId 规则id
     * @param thinAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    @Override
    public Response<Boolean> batchUpdate(Long ruleId, List<ThinAddress> thinAddresses) {
        try {
            WarehouseRule rule = warehouseRuleDao.findById(ruleId);
            if(rule == null){
                log.error("warehouse rule(id={}) not found", ruleId);
                return Response.fail("warehouse.rule.not.found");
            }
            Long shopGroupId = rule.getShopGroupId();
            warehouseAddressRuleManager.batchUpdate(shopGroupId, ruleId, thinAddresses);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to update warehouseAddressRule(ruleId={}) with address:{}, cause:{}",
                    ruleId, thinAddresses, Throwables.getStackTraceAsString(e));
            return Response.fail("address.may.conflict");
        }
    }


    @Override
    public Response<Boolean> deleteByRuleId(Long ruleId) {
        try {
            warehouseAddressRuleManager.deleteByRuleId(ruleId);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete warehouseAddressRule failed, warehouseAddressRuleId:ruleId={}, cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.rule.delete.fail");
        }
    }


}
