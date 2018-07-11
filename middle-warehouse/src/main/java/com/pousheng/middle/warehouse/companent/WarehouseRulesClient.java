package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 派单规则API调用
 *
 * @auther feisheng.ch
 * @time 2018/5/23
 */
@Component
@Slf4j
public class WarehouseRulesClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 创建新的派单规则
     * @param shops
     * @return
     */
    public Response<Long> createRule(List<ThinShop> shops) {
        if (null == shops) {
            return Response.fail("parameter.fail");
        }

        try {
            return Response.ok((Long) inventoryBaseClient.postJson("api/inventory/rule",
                    JSON.toJSONString(shops), Long.class));

        } catch (Exception e) {
            log.error("create warehouse rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 更新派单规则
     * @param ruleId
     * @param shops
     * @return
     */
    public Response<Boolean> updateRule(Long ruleId, List<ThinShop> shops) {
        if (null == ruleId || null == shops) {
            return Response.fail("parameter.fail");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.putJson("api/inventory/rule/"+ruleId,
                    JSON.toJSONString(shops), Boolean.class));

        } catch (Exception e) {
            log.error("update warehouse rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 删除派单规则
     * @param ruleId
     * @return
     */
    public Response<Boolean> deleteById(Long ruleId) {
        if (null == ruleId) {
            return Response.fail("parameter.fail");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.delete("api/inventory/rule/"+ruleId,
                    Maps.newHashMap(), Boolean.class));

        } catch (Exception e) {
            log.error("delete warehouse rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 删除派单规则店铺组
     * @param groupId
     * @return
     */
    public Response<Boolean> deleteShopGroup(Long groupId) {
        if (null == groupId) {
            return Response.fail("parameter.fail");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.delete("api/inventory/rule/group/"+groupId,
                    Maps.newHashMap(), Boolean.class));

        } catch (Exception e) {
            log.error("delete warehouse rule shop group fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 查询所有配置过的店铺ID列表
     * @return
     */
    public Set<Long> findAllConfigShopIds () {
        try {
            List<Long> ret = (List<Long>) inventoryBaseClient.get("api/inventory/rule/allConfigShopIds",
                    null, null, Maps.newHashMap(), Long.class, true);

            if (null != ret) {
                Set<Long> set = Sets.newHashSet();
                set.addAll(ret);

                return set;
            }
        } catch (Exception e) {
            log.error("find all config shop id list fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return Sets.newHashSet();
    }

    /**
     * 根据ID获取规则
     * @param ruleId
     * @return
     */
    public WarehouseRule findRuleById (Long ruleId) {
        try {
            return (WarehouseRule) inventoryBaseClient.get("api/inventory/rule/"+ruleId,
                    null, null, Maps.newHashMap(), WarehouseRule.class, false);
        } catch (Exception e) {
            log.error("find shop rule by id fail, ruleId:{}, cause:{}", ruleId, Throwables.getStackTraceAsString(e));
        }

        return null;
    }

    /**
     * 根据组ID获取shop列表
     * @param shopGroupId
     * @return
     */
    public List<WarehouseShopGroup> findShopGroupById (Long shopGroupId) {
        try {
            return (List<WarehouseShopGroup>) inventoryBaseClient.get("api/inventory/rule/findShopGroupsById/"+shopGroupId,
                    null, null, Maps.newHashMap(), WarehouseShopGroup.class, true);
        } catch (Exception e) {
            log.error("find shop group by id fail, shopGroupId:{}, cause:{}", shopGroupId, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();
    }

    /**
     * 根据组ID获取所属店铺列表
     * @param groupId
     * @return
     */
    public List<WarehouseShopGroup> findShopListByGroup (Long groupId) {
        try {
            if (null == groupId) {
                return Lists.newArrayList();
            }

            return (List<WarehouseShopGroup>) inventoryBaseClient.get("api/inventory/rule/findShopListByGroup/"+groupId,
                    null, null, Maps.newHashMap(), WarehouseShopGroup.class, true);
        } catch (Exception e) {
            log.error("find shop list by group id fail, groupId:{}, cause:{}", groupId, Throwables.getStackTraceAsString(e));
        }

        return Lists.newArrayList();
    }

    /**
     * 根据店铺id查找设置的仓库列表
     *
     * @param openShopId
     * @return
     */

    public Response<List<Long>> findWarehouseIdsByShopId (Long openShopId) {
        try {
            if (null == openShopId) {
                return Response.ok(Lists.newArrayList());
            }

            return Response.ok((List<Long>) inventoryBaseClient.get("api/inventory/rule/findWarehouseIdsByShopId/"+openShopId,
                    null, null, Maps.newHashMap(), Long.class, true));
        } catch (Exception e) {
            log.error("failed to find warehouseIds By ShopId, openShopId:{}, cause:{}", openShopId, Throwables.getStackTraceAsString(e));
        }

        return Response.ok(Lists.newArrayList());
    }

}
