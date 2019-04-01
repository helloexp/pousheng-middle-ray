package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.ShopWarehouseStockRule;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 仓库库存推送规则API调用
 *
 * @author zxw
 * @time 2018/9/13
 */
@Component
@Slf4j
public class ShopWarehouseRuleClient {

    private static final String API_PATH_PREFIX = "api/inventory/shop-warehouse-rule";

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 创建新的仓库推送规则
     *
     * @param shopWarehouseStockRule
     * @return
     */

    public Response<Long> createShopWarehouseRule(@LogMeContext ShopWarehouseStockRule shopWarehouseStockRule) {
        if (null == shopWarehouseStockRule) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }

        try {
            return Response.ok((Long) inventoryBaseClient.postJson(API_PATH_PREFIX,
                    JSON.toJSONString(shopWarehouseStockRule), Long.class));

        } catch (Exception e) {
            log.error("create shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 更新仓库推送规则
     *
     * @param shopWarehouseStockRule
     * @return
     */
    public Response<Boolean> updateShopWarehouseRule(@LogMeContext ShopWarehouseStockRule shopWarehouseStockRule) {
        if (null == shopWarehouseStockRule) {
            return Response.fail("warehouse.shop.rule.update.fail.parameter");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.putJson(API_PATH_PREFIX + "/" + shopWarehouseStockRule.getId(),
                    JSON.toJSONString(shopWarehouseStockRule), Boolean.class));

        } catch (Exception e) {
            log.error("update shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 根据ID获取规则信息
     *
     * @return
     */
    public ShopWarehouseStockRule findById(Long id) {
        try {
            return (ShopWarehouseStockRule) inventoryBaseClient.get(API_PATH_PREFIX + "/" + id,
                    null, null, Maps.newHashMap(), ShopWarehouseStockRule.class, false);

        } catch (Exception e) {
            log.error("find shop sku rule by id fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return null;
    }


    /**
     * 根据店铺ID和skucode(模糊)列表获取配置过的店铺及规则数据
     *
     * @return
     */
    public Map<Long, ShopWarehouseStockRule> findByShopId(Long shopId) {
        try {
            List<ShopWarehouseStockRule> rules = (List<ShopWarehouseStockRule>) inventoryBaseClient.get(API_PATH_PREFIX + "/findByShopId/" + shopId,
                    null, null, Maps.newHashMap(), ShopWarehouseStockRule.class, true);
            if (null != rules) {
                return rules.stream().filter(Objects::nonNull)
                        .collect(Collectors.toMap(ShopWarehouseStockRule::getWarehouseId, it -> it));
            }

        } catch (Exception e) {
            log.error("find all config shop warehouse list fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return Maps.newHashMap();
    }


    /**
     * 创建新的仓库推送规则
     *
     * @param rule
     * @return
     */
    @LogMe(description = "批量编辑仓库级库存推送规则", ignore = true)
    public Response<Boolean> createOrUpdate(@LogMeContext ShopWarehouseStockRule rule) {
        log.info("create shop sku rule , shopWarehouseSkuStockRules is {}", JSON.toJSONString(rule));
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("shopWarehouseStockRule", JSON.toJSONString(rule));
            Boolean result = (Boolean) inventoryBaseClient.post(API_PATH_PREFIX + "/createOrUpdate",
                    params, Boolean.class);
            return Response.ok(result);

        } catch (Exception e) {
            log.error("create shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

}
