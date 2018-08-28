package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.WarehouseShopSkuStockRule;
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
public class WarehouseShopSkuRuleClient {

    private static final String API_PATH_PREFIX = "api/inventory/shop-sku-rule";

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 创建新的商品推送规则
     *
     * @param warehouseShopSkuStockRule
     * @return
     */
    public Response<Long> createShopSkuRule(WarehouseShopSkuStockRule warehouseShopSkuStockRule) {
        if (null == warehouseShopSkuStockRule) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }

        try {
            return Response.ok((Long) inventoryBaseClient.postJson(API_PATH_PREFIX + "/create",
                    JSON.toJSONString(warehouseShopSkuStockRule), Long.class));

        } catch (Exception e) {
            log.error("create shop sku rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 更新商品推送规则
     *
     * @param warehouseShopSkuStockRule
     * @return
     */
    public Response<Boolean> updateShopSkuRule(WarehouseShopSkuStockRule warehouseShopSkuStockRule) {
        if (null == warehouseShopSkuStockRule) {
            return Response.fail("warehouse.shop.rule.update.fail.parameter");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.putJson(API_PATH_PREFIX + "/" + warehouseShopSkuStockRule.getId(),
                    JSON.toJSONString(warehouseShopSkuStockRule), Boolean.class));

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
    public WarehouseShopSkuStockRule findById(Long id) {
        try {
            return (WarehouseShopSkuStockRule) inventoryBaseClient.get(API_PATH_PREFIX + "/" + id,
                    null, null, Maps.newHashMap(), WarehouseShopSkuStockRule.class, false);

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
    public Response<List<String>> batchCreateOrUpdate(List<WarehouseShopSkuStockRule> rules) {
        if (StringUtils.isEmpty(rules)) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }
        log.info("create shop sku rule , warehouseShopSkuStockRules is {}", JSON.toJSONString(rules));
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("warehouseShopSkuStockRules", JSON.toJSONString(rules));
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
    public Map<String, WarehouseShopSkuStockRule> findSkuRules(ItemMappingCriteria criteria) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("shopId", criteria.getOpenShopId());
            params.put("skuCodeFluz", criteria.getSkuCode());
            if (!StringUtils.isEmpty(criteria.getSkuCodes())) {
                params.put("skuCodes", Joiners.COMMA.join(criteria.getSkuCodes()));
            }

            List<WarehouseShopSkuStockRule> rules = (List<WarehouseShopSkuStockRule>) inventoryBaseClient.get("api/inventory/shop-sku-rule/findSkuRules",
                    null, null, params, WarehouseShopSkuStockRule.class, true);

            if (null != rules) {
                return rules.stream().filter(Objects::nonNull)
                        .collect(Collectors.toMap(WarehouseShopSkuStockRule::getSkuCode, it -> it));
            }

        } catch (Exception e) {
            log.error("find all config shop id list fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return Maps.newHashMap();
    }

}
