package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 库存推送规则API调用
 *
 * @auther feisheng.ch
 * @time 2018/5/23
 */
@Component
@Slf4j
public class WarehouseShopRuleClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 创建新的店铺推送规则
     *
     * @param warehouseShopStockRule
     * @return
     */
    public Response<Long> createShopRule(ShopStockRule warehouseShopStockRule) {
        if (null == warehouseShopStockRule) {
            return Response.fail("warehouse.shop.rule.create.fail.parameter");
        }

        try {
            return Response.ok((Long) inventoryBaseClient.postJson("api/inventory/shop-rule",
                    JSON.toJSONString(warehouseShopStockRule), Long.class));

        } catch (Exception e) {
            log.error("create shop rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

    /**
     * 更新店铺推送规则
     *
     * @param warehouseShopStockRule
     * @return
     */
    public Response<Boolean> updateShopRule(ShopStockRule warehouseShopStockRule) {
        if (null == warehouseShopStockRule) {
            return Response.fail("warehouse.shop.rule.update.fail.parameter");
        }

        try {
            return Response.ok((Boolean) inventoryBaseClient.putJson("api/inventory/shop-rule/" + warehouseShopStockRule.getId(),
                    JSON.toJSONString(warehouseShopStockRule), Boolean.class));

        } catch (Exception e) {
            log.error("update shop rule fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 根据ID获取规则信息
     *
     * @return
     */
    public ShopStockRule findById(Long id) {
        try {
            return (ShopStockRule) inventoryBaseClient.get("api/inventory/shop-rule/" + id,
                    null, null, Maps.newHashMap(), ShopStockRule.class, false);

        } catch (Exception e) {
            log.error("find shop rule by id fail, id: {}, cause:{}", id, Throwables.getStackTraceAsString(e));
        }

        return null;
    }

    /**
     * 根据shopId获取规则信息
     *
     * @return
     */
    public ShopStockRule findByShopId(Long shopId) {
        try {
            return (ShopStockRule) inventoryBaseClient.get("api/inventory/shop-rule/findByShopId/" + shopId,
                    null, null, Maps.newHashMap(), ShopStockRule.class, false);

        } catch (Exception e) {
            log.error("find shop rule by shopId fail, shopId: {}, cause:{}", shopId, Throwables.getStackTraceAsString(e));
        }

        return null;
    }

    /**
     * 根据根据shopId和skucode获取规则信息
     *
     * @param shopId
     * @param skuCode
     * @return
     */
    public Response<ShopStockRuleDto> findByShopIdAndSku(Long shopId, String skuCode) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("shopId", shopId);
            params.put("skuCode", skuCode);
            return Response.ok((ShopStockRuleDto) inventoryBaseClient.get("api/inventory/shop-rule/findByShopIdAndSku",
                    null, null, params, ShopStockRuleDto.class, false));
        } catch (Exception e) {
            log.info("find shop rule by shop and skuCode fail, shopId:{}, skuCode:{}, cause:{}", shopId, skuCode, e.getMessage());

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 根据条件分页
     *
     * @return
     */
    public Paging<ShopStockRule> shopRulePagination(Integer pageNo, Integer pageSize, List<Long> shopIds) {
        try {
            Map<String, Object> params = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(shopIds)) {
                params.put("shopIds", JSON.toJSONString(shopIds));
            }
            Paging<ShopStockRule> rulePaging = (Paging<ShopStockRule>) inventoryBaseClient.get("api/inventory/shop-rule/paging",
                    pageNo, pageSize, params, Paging.class, false);

            if (null != rulePaging) {
                return rulePaging;
            }

        } catch (Exception e) {
            log.error("shop rule pagination fail, cause:{}", Throwables.getStackTraceAsString(e));
        }

        return Paging.empty();
    }

    /**
     * 根据仓库编号查询涉及的店铺编号列表
     * @param warehouseId
     * @return
     */
    public Response<List<Long>> findShopIdsByWarehouseId(Long warehouseId) {
        try {
            return Response.ok(
                (List<Long>)inventoryBaseClient.get("api/inventory/rule/findShopIdsByWarehouseId/" + warehouseId,
                    null, null, Maps.newHashMap(), Long.class, true));
        } catch (Exception e) {
            log.info("find shop rule by warehouse id fail, warehouseId:{}", warehouseId, e);
            return Response.fail(e.getMessage());
        }
    }

}
