package com.pousheng.middle.group.impl.manager;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.dao.ItemRuleWarehouseDao;
import com.pousheng.middle.group.model.ItemRule;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.model.ItemRuleShop;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import com.pousheng.middle.item.enums.ItemRuleType;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
@Component
@Slf4j
public class ItemRuleManager {

    @Autowired
    private ItemRuleDao itemRuleDao;

    @Autowired
    private ItemRuleGroupDao itemRuleGroupDao;

    @Autowired
    private ItemRuleShopDao itemRuleShopDao;

    @Autowired
    private ItemRuleWarehouseDao itemRuleWarehouseDao;

    @Transactional
    public Response<Long> createWithShops(List<Long> shopIds) {
        try {
            ItemRule itemRule = new ItemRule().type(ItemRuleType.SHOP.value());
            if (!itemRuleDao.create(itemRule)) {
                return Response.fail("item.rule.create.fail");
            }
            List<ItemRuleShop> list = Lists.newArrayList();
            for (Long shopId : shopIds) {
                list.add(new ItemRuleShop().ruleId(itemRule.getId()).shopId(shopId));
            }
            itemRuleShopDao.creates(list);
            return Response.ok(itemRule.getId());
        } catch (Exception e) {
            log.error("create itemRule failed, shopIds:{}, cause:{}", shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.create.fail");
        }
    }

    @Transactional
    public Response<Long> createWithWarehouses(List<Long> warehouseIds) {
        try {
            ItemRule itemRule = new ItemRule().type(ItemRuleType.WAREHOUSE.value());
            if (!itemRuleDao.create(itemRule)) {
                return Response.fail("item.rule.create.fail");
            }
            List<ItemRuleWarehouse> list = Lists.newArrayList();
            for (Long warehouseId : warehouseIds) {
                list.add(new ItemRuleWarehouse().ruleId(itemRule.getId()).warehouseId(warehouseId));
            }
            itemRuleWarehouseDao.creates(list);
            return Response.ok(itemRule.getId());
        } catch (Exception e) {
            log.error("create itemRule failed, warehouseIds:{}, cause:{}", warehouseIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.create.fail");
        }
    }


    @Transactional
    public Response<Boolean> updateShops(Long ruleId, List<Long> shopIds) {
        try {
            if(shopIds.isEmpty()){
                itemRuleShopDao.deleteByRuleId(ruleId);
                return Response.ok(true);
            }
            List<ItemRuleShop> list = Lists.newArrayList();
            for (Long shopId : shopIds) {
                list.add(new ItemRuleShop().ruleId(ruleId).shopId(shopId));
            }
            itemRuleShopDao.deleteByRuleId(ruleId);
            itemRuleShopDao.creates(list);
            return Response.ok(true);
        } catch (Exception e) {
            log.error("update itemRule shop failed,ruleId:{} shopIds:{}, cause:{}",
                    ruleId, shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.update.fail");
        }
    }


    @Transactional
    public Response<Boolean> updateWarehouses(Long ruleId, List<Long> warehouseIds) {
        try {
            if(warehouseIds.isEmpty()){
                itemRuleWarehouseDao.deleteByRuleId(ruleId);
                return Response.ok(true);
            }
            List<ItemRuleWarehouse> list = Lists.newArrayList();
            for (Long warehouseId : warehouseIds) {
                list.add(new ItemRuleWarehouse().ruleId(ruleId).warehouseId(warehouseId));
            }
            itemRuleWarehouseDao.deleteByRuleId(ruleId);
            itemRuleWarehouseDao.creates(list);
            return Response.ok(true);
        } catch (Exception e) {
            log.error("update itemRule warehouse failed,ruleId:{} warehouseIds:{}, cause:{}",
                    ruleId, warehouseIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.update.fail");
        }
    }


    @Transactional
    public Response<Boolean> updateGroups(Long ruleId, List<Long> groupIds) {
        try {
            if(groupIds.isEmpty()){
                itemRuleGroupDao.deleteByRuleId(ruleId);
                return Response.ok(true);
            }
            List<ItemRuleGroup> list = Lists.newArrayList();
            for (Long groupId : groupIds) {
                list.add(new ItemRuleGroup().ruleId(ruleId).groupId(groupId));
            }
            itemRuleGroupDao.deleteByRuleId(ruleId);
            itemRuleGroupDao.creates(list);
            return Response.ok(true);
        } catch (Exception e) {
            log.error("update itemRule failed, ruleId:{}, groupIds:{} , cause:{}",
                    ruleId, groupIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.update.fail");
        }
    }


    @Transactional
    public Response<Boolean> deleteById(Long itemRuleId) {
        try {
            Boolean resp = itemRuleDao.delete(itemRuleId);
            if (!resp) {
                return Response.fail("item.rule.delete.fail");
            }
            itemRuleGroupDao.deleteByRuleId(itemRuleId);
            itemRuleShopDao.deleteByRuleId(itemRuleId);
            itemRuleWarehouseDao.deleteByRuleId(itemRuleId);
            return Response.ok(resp);
        } catch (Exception e) {
            log.error("delete itemRule failed, itemRuleId:{}, cause:{}", itemRuleId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.delete.fail");
        }
    }


}
