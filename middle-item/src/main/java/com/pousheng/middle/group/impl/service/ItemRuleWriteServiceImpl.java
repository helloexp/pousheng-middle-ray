package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.impl.manager.ItemRuleManager;
import com.pousheng.middle.group.model.ItemRule;
import com.pousheng.middle.group.service.ItemRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Slf4j
@Service
@RpcProvider
public class ItemRuleWriteServiceImpl implements ItemRuleWriteService {

    @Autowired
    private ItemRuleDao itemRuleDao;

    @Autowired
    private ItemRuleManager itemRuleManager;

    @Override
    public Response<Long> create(ItemRule itemRule) {
        try {
            itemRuleDao.create(itemRule);
            return Response.ok(itemRule.getId());
        } catch (Exception e) {
            log.error("create itemRule failed, itemRule:{}, cause:{}", itemRule, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemRule itemRule) {
        try {
            return Response.ok(itemRuleDao.update(itemRule));
        } catch (Exception e) {
            log.error("update itemRule failed, itemRule:{}, cause:{}", itemRule, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long itemRuleId) {
        try {
            return Response.ok(itemRuleDao.delete(itemRuleId));
        } catch (Exception e) {
            log.error("delete itemRule failed, itemRuleId:{}, cause:{}", itemRuleId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.delete.fail");
        }
    }

    @Override
    public Response<Long> createWithShop(List<Long> shopIds) {
        return itemRuleManager.createWithShops(shopIds);
    }

    @Override
    public Response<Long> createWithWarehouse(List<Long> warehouseIds) {
        return itemRuleManager.createWithWarehouses(warehouseIds);
    }

    @Override
    public Response<Boolean> updateShops(Long ruleId, List<Long> shopIds) {
        return itemRuleManager.updateShops(ruleId, shopIds);
    }

    @Override
    public Response<Boolean> updateWarehouses(Long ruleId, List<Long> warehouseIds) {
        return itemRuleManager.updateWarehouses(ruleId, warehouseIds);
    }

    @Override
    public Response<Boolean> updateGroups(Long ruleId, List<Long> groupIds) {
        return itemRuleManager.updateGroups(ruleId, groupIds);
    }

    @Override
    public Response<Boolean> delete(Long itemRuleId) {
        return itemRuleManager.deleteById(itemRuleId);
    }


}
