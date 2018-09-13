package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.ShopWarehouseSkuStockRule;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.common.utils.Joiners;
import io.terminus.open.client.common.dto.ItemMappingCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品库存推送规则API调用
 *
 * @auther feisheng.ch
 * @time 2018/5/23
 */
@Component
@Slf4j
public class ShopWarehouseSkuRuleClient {

    private static final String API_PATH_PREFIX = "api/inventory/shop-sku-rule";

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 创建新的商品推送规则
     *
     * @param shopWarehouseSkuStockRule
     * @return
     */
    public Response<Long> createShopSkuRule(ShopWarehouseSkuStockRule shopWarehouseSkuStockRule) {
        if (null == shopWarehouseSkuStockRule) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }

        try {
            return Response.ok((Long) inventoryBaseClient.postJson(API_PATH_PREFIX + "/create",
                    JSON.toJSONString(shopWarehouseSkuStockRule), Long.class));

        } catch (Exception e) {
            log.error("create shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 更新商品推送规则
     *
     * @param shopWarehouseSkuStockRule
     * @return
     */
    public Response<Boolean> updateShopSkuRule(ShopWarehouseSkuStockRule shopWarehouseSkuStockRule) {
        if (null == shopWarehouseSkuStockRule) {
            return Response.fail("warehouse.shop.rule.update.fail.parameter");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.putJson(API_PATH_PREFIX + "/" + shopWarehouseSkuStockRule.getId(),
                    JSON.toJSONString(shopWarehouseSkuStockRule), Boolean.class));

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
    public ShopWarehouseSkuStockRule findById(Long id) {
        try {
            return (ShopWarehouseSkuStockRule) inventoryBaseClient.get(API_PATH_PREFIX + "/" + id,
                    null, null, Maps.newHashMap(), ShopWarehouseSkuStockRule.class, false);

        } catch (Exception e) {
            log.error("find shop sku rule by id fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return null;
    }


    /**
     * 创建新的商品推送规则
     *
     * @param rules
     * @return
     */
    @LogMe(description = "批量编辑商品级推送规则", ignore = true)
    public Response<List<String>> batchCreateOrUpdate(@LogMeContext  List<ShopWarehouseSkuStockRule> rules) {
        if (StringUtils.isEmpty(rules)) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }
        log.info("create shop sku rule , shopWarehouseSkuStockRules is {}", JSON.toJSONString(rules));
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("shopWarehouseSkuStockRules", JSON.toJSONString(rules));
            String result = (String) inventoryBaseClient.post(API_PATH_PREFIX + "/batchCreateOrUpdate",
                    params, String.class);
            if (StringUtils.isEmpty(result)) {
                return Response.ok(Lists.newArrayList());
            }
            log.info("create shop sku rule fail list {}", result);
            return Response.ok(Splitters.COMMA.splitToList(result));

        } catch (Exception e) {
            log.error("create shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 根据店铺ID和skucode(模糊)列表获取配置过的店铺及规则数据
     *
     * @return
     */
    public Map<String, ShopWarehouseSkuStockRule> findSkuRules(Long warehouseId, ItemMappingCriteria criteria) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("shopId", criteria.getOpenShopId());
            params.put("skuCodeFluz", criteria.getSkuCode());
            params.put("warehouseId", warehouseId);
            if (!StringUtils.isEmpty(criteria.getSkuCodes())) {
                params.put("skuCodes", Joiners.COMMA.join(criteria.getSkuCodes()));
            }

            List<ShopWarehouseSkuStockRule> rules = (List<ShopWarehouseSkuStockRule>) inventoryBaseClient.get("api/inventory/shop-sku-rule/findSkuRules",
                    null, null, params, ShopWarehouseSkuStockRule.class, true);

            if (null != rules) {
                return rules.stream().filter(Objects::nonNull)
                        .collect(Collectors.toMap(ShopWarehouseSkuStockRule::getSkuCode, it -> it));
            }

        } catch (Exception e) {
            log.error("find all config shop id list fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return Maps.newHashMap();
    }

}
