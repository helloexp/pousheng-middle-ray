package com.pousheng.middle.warehouse.manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
// TODO 部分失败补偿机制
public class WarehouseSkuStockManager {

    private final InventoryClient inventoryClient;

    @Autowired
    public WarehouseSkuStockManager(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    /**
     * 锁定库存
     *
     * @param warehouses 待锁定的库存明细
     */
    public Response<Boolean> lockStock(InventoryTradeDTO inventoryTradeDTO, List<WarehouseShipment> warehouses) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (WarehouseShipment warehouseShipment : warehouses) {
            tradeList.addAll(genTradeContextList(warehouseShipment.getWarehouseId(),
                    inventoryTradeDTO, warehouseShipment.getSkuCodeAndQuantities()));
        }

        if (!ObjectUtils.isEmpty(tradeList)) {
            Response<Boolean> tradeRet = inventoryClient.lock(tradeList);
            if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                log.error("fail to occupy inventory, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, warehouses, tradeRet.getError());
                return Response.fail(tradeRet.getError());
            }
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 先解锁之前的锁定的库存, 在扣减实际发货的库存
     *
     * @param actualShipments 实际发货的库存明细
     */
    public Response<Boolean> decreaseStock(InventoryTradeDTO inventoryTradeDTO, List<WarehouseShipment> actualShipments) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (WarehouseShipment actualShipment : actualShipments) {
            tradeList.addAll(genTradeContextList(actualShipment.getWarehouseId(),
                    inventoryTradeDTO, actualShipment.getSkuCodeAndQuantities()));
        }

        if (!ObjectUtils.isEmpty(tradeList)) {
            Response<Boolean> tradeRet = inventoryClient.decrease(tradeList);
            if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                log.error("fail to decrease inventory, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, actualShipments, tradeRet.getError());
                return Response.fail(tradeRet.getError());
            }
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 根据指定的仓库分配策略解锁库存, 当撤销发货单时, 调用这个接口
     *
     * @param warehouseShipments 仓库及解锁数量列表
     */
    public Response<Boolean> unlockStock(InventoryTradeDTO inventoryTradeDTO, List<WarehouseShipment> warehouseShipments) {
        return doUnlock(inventoryTradeDTO, warehouseShipments);
    }

    private Response<Boolean> doUnlock(InventoryTradeDTO inventoryTradeDTO, List<WarehouseShipment> lockedShipments) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (WarehouseShipment lockedShipment : lockedShipments) {
            tradeList.addAll(genTradeContextList(lockedShipment.getWarehouseId(),
                    inventoryTradeDTO, lockedShipment.getSkuCodeAndQuantities()));
        }

        if (!ObjectUtils.isEmpty(tradeList)) {
            Response<Boolean> tradeRet = inventoryClient.unLock(tradeList);
            if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                log.error("fail to unLock inventory, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, lockedShipments, tradeRet.getError());
                return Response.fail(tradeRet.getError());
            }
        }

        return Response.ok(Boolean.TRUE);
    }

    private List<InventoryTradeDTO> genTradeContextList (Long warehouseId, InventoryTradeDTO inventoryTradeDTO, List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();
            Integer quantity = skuCodeAndQuantity.getQuantity();

            InventoryTradeDTO currTrade = new InventoryTradeDTO();
            currTrade.setWarehouseId(warehouseId);
            currTrade.setQuantity(quantity);
            currTrade.setSkuCode(skuCode);
            currTrade.setOrderId(inventoryTradeDTO.getOrderId());
            currTrade.setSubOrderId(Lists.newArrayList(inventoryTradeDTO.getSubOrderId()));
            currTrade.setShopId(inventoryTradeDTO.getShopId());
            currTrade.setUniqueCode(inventoryTradeDTO.getUniqueCode());

            tradeList.add(currTrade);

        }

        return tradeList;
    }

}
