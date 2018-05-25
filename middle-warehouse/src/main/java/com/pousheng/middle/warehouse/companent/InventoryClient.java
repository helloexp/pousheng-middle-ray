package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 库存操作相关接口
 *
 * @auther feisheng.ch
 * @time 2018/5/25
 */
@Component
@Slf4j
public class InventoryClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 锁定（对应库存中心的占用操作）
     * @return
     */
    public Response<Boolean> lock (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/occupy");
    }

    /**
     * 解锁（对应库存中心的解除占用操作）
     * @return
     */
    public Response<Boolean> unLock (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/unOccupy");
    }

    /**
     * 扣减（对应库存中心的扣减操作）
     * @return
     */
    public Response<Boolean> decrease (List<InventoryTradeDTO> inventoryTradeDTOList) {
        return trade(inventoryTradeDTOList, "api/inventory/trade/decrease");
    }

    private Response<Boolean> trade (List<InventoryTradeDTO> inventoryTradeDTOList, String apiPath) {
        if (ObjectUtils.isEmpty(inventoryTradeDTOList)) {
            return Response.fail("inventory.trade.fail.parameter");
        }
        try {
            return Response.ok((Boolean) inventoryBaseClient.post(apiPath,
                    ImmutableMap.of("tradeDTO", JSON.toJSONString(inventoryTradeDTOList)), Boolean.class));
        } catch (Exception e) {
            log.error("fail to trade inventory, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取可用库存数量
     *
     * @param requests
     * @param shopId
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailableInventory (List<AvailableInventoryRequest> requests, Long shopId) {
        if (ObjectUtils.isEmpty(requests)) {
            return Response.fail("inventory.available.fail.parameter");
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("shopId", shopId);
        params.put("requestJson", JSON.toJSONString(requests));

        try {
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient.get("api/inventory/query/getAvailableInventory",
                    null, null, params, AvailableInventoryDTO.class, true);

            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }

            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取可用库存数量，忽略仓库，返回列表中也没有仓库ID信息
     * @param requests
     * @param shopId
     * @return
     */
    public Response<List<AvailableInventoryDTO>> getAvailInvRetNoWarehouse (List<AvailableInventoryRequest> requests, Long shopId) {
        if (ObjectUtils.isEmpty(requests)) {
            return Response.fail("inventory.available.fail.parameter");
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("shopId", shopId);
        params.put("requestJson", JSON.toJSONString(requests));

        try {
            List<AvailableInventoryDTO> availableInvList = (List<AvailableInventoryDTO>)inventoryBaseClient.get("api/inventory/query/getAvailInvRetNoWarehouse",
                    null, null, params, AvailableInventoryDTO.class, true);

            if (ObjectUtils.isEmpty(availableInvList)) {
                return Response.ok(Lists.newArrayList());
            }

            return Response.ok(availableInvList);
        } catch (Exception e) {
            log.error("get available inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 查询某个sku在指定仓库的库存情况
     *
     * @param warehouseId
     * @param skuCode
     * @return
     */
    public Response<InventoryDTO> findByWarehouseIdAndSkuCode (Long warehouseId, String skuCode) {
        if (null == warehouseId && ObjectUtils.isEmpty(skuCode)) {
            return Response.fail("inventory.find.fail.parameter");
        }
        try {
            Response<List<InventoryDTO>> inventoryRes = findSkuStocks(warehouseId, Lists.newArrayList(skuCode));
            if (inventoryRes.isSuccess() && ObjectUtils.isEmpty(inventoryRes.getResult())) {
                InventoryDTO inv = new InventoryDTO();
                inv.setWarehouseId(warehouseId);
                inv.setSkuCode(skuCode);
                inv.setRealQuantity(0L);
                inv.setPreorderQuantity(0L);
                inv.setWithholdQuantity(0L);
                inv.setOccupyQuantity(0L);
                inv.setSafeQuantity(0L);

                return Response.ok(inv);
            }

            return Response.ok(inventoryRes.getResult().get(0));
        } catch (Exception e) {
            log.error("get fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取多个sku在指定仓库下的库存情况
     *
     * @param warehouseId
     * @param skuCodeList
     * @return
     */
    public Response<List<InventoryDTO>> findSkuStocks (Long warehouseId, List<String> skuCodeList) {
        if (null == warehouseId && ObjectUtils.isEmpty(skuCodeList)) {
            return Response.fail("inventory.find.fail.parameter");
        }
        try {
            Map<String, Object> params = Maps.newHashMap();
            params.put("warehouseId", warehouseId);
            params.put("skuCodeList", (null==skuCodeList?JSON.toJSONString(Lists.newArrayList()):JSON.toJSONString(skuCodeList)));

            return Response.ok((List<InventoryDTO>) inventoryBaseClient.get(
                    "api/inventory/query/findInventoryByWarehouseSkus",
                    null, null, params, InventoryDTO.class, true));

        } catch (Exception e) {
            log.error("find all config shop id list fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取指定sku的库存列表
     * @param skuCode
     * @return
     */
    public Response<List<InventoryDTO>> findBySkuCode (String skuCode) {
        return findSkuStocks(null, Lists.newArrayList(skuCode));
    }


}
