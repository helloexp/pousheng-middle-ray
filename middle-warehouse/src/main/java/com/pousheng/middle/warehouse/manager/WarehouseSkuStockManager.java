package com.pousheng.middle.warehouse.manager;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.event.TaskLockEvent;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.ShipmentItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
// TODO 部分失败补偿机制
public class WarehouseSkuStockManager {

    private final InventoryClient inventoryClient;

    private final PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private EventBus eventBus;

    @Autowired
    public WarehouseSkuStockManager(InventoryClient inventoryClient, PoushengCompensateBizWriteService poushengCompensateBizWriteService) {
        this.inventoryClient = inventoryClient;
        this.poushengCompensateBizWriteService = poushengCompensateBizWriteService;
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
                log.info("tradeRet getError is {}", tradeRet.getError());
                if (Objects.equals(tradeRet.getError(),"inventory.response.timeout")) {
                    log.info("begin to create lock task, shipment id is {}", Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                    // 超时的错误进入biz表给后面轮询
//                    createShipmentResultTask(Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                    eventBus.post(new TaskLockEvent(Long.parseLong(inventoryTradeDTO.getBizSrcId())));
                }
                return Response.fail(tradeRet.getError());
            }
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 手工派单走的锁定接口，请慎用，锁定库存
     *
     * @param warehouses 待锁定的库存明细
     */
    public Response<Boolean> lockStockUserDispatch(InventoryTradeDTO inventoryTradeDTO, List<WarehouseShipment> warehouses) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (WarehouseShipment warehouseShipment : warehouses) {
            tradeList.addAll(genTradeContextList(warehouseShipment.getWarehouseId(),
                    inventoryTradeDTO, warehouseShipment.getSkuCodeAndQuantities()));
        }

        if (!ObjectUtils.isEmpty(tradeList)) {
            Response<Boolean> tradeRet = inventoryClient.lockUserDispatch(tradeList);
            if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                log.error("fail to occupy inventory for user dispatch logic, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, warehouses, tradeRet.getError());
                if (Objects.equals(tradeRet.getError(),"inventory.response.timeout")) {
                    // 超时的错误进入biz表给后面轮询
//                    createShipmentResultTask(Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                    eventBus.post(new TaskLockEvent(Long.parseLong(inventoryTradeDTO.getBizSrcId())));
                }
                return Response.fail(tradeRet.getError());
            }
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 先解锁之前的锁定的库存, 在扣减实际发货的库存
     *
     * @param actualShipment 实际发货的库存明细
     */
    public Response<Boolean> decreaseStock(InventoryTradeDTO inventoryTradeDTO, WarehouseShipment actualShipment, List<ShipmentItem> items) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        List<InventoryTradeDTO> releaseList = Lists.newArrayList();
        List<SkuCodeAndQuantity> unlockList = Lists.newArrayList();
        Map<Long, Integer> itemMap = items.stream().filter(e -> e.getShipQuantity() != null)
                .collect(Collectors.toMap(ShipmentItem::getSkuOrderId, ShipmentItem::getShipQuantity));
        for (SkuCodeAndQuantity sq : actualShipment.getSkuCodeAndQuantities()) {
            if (itemMap.get(sq.getSkuOrderId()) == null) {
                continue;
            }
            if (itemMap.get(sq.getSkuOrderId()) < sq.getQuantity()) {
                unlockList.add(new SkuCodeAndQuantity().skuOrderId(sq.getSkuOrderId()).skuCode(sq.getSkuCode())
                        .quantity(sq.getQuantity() - itemMap.get(sq.getSkuOrderId())));
            }
            sq.setQuantity(itemMap.get(sq.getSkuOrderId()));
        }
        tradeList.addAll(genTradeContextList(actualShipment.getWarehouseId(),
                inventoryTradeDTO, actualShipment.getSkuCodeAndQuantities()));

        //这里增加如果没发全的判断 没发全要做扣减加释放
        if (!ObjectUtils.isEmpty(tradeList)) {
            Response<Boolean> tradeRet = inventoryClient.decrease(tradeList);
            if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                log.error("fail to decrease inventory, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, actualShipment, tradeRet.getError());
                // if (Objects.equals(tradeRet.getError(),"inventory.response.timeout")) {
                    // 超时的错误进入biz表给后面轮询,扣减库存biz
                    createDecreaseStockTask(Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                // }
                return Response.fail(tradeRet.getError());
            }
        }
        if (!ObjectUtils.isEmpty(unlockList)) {
            releaseList.addAll(genTradeContextList(actualShipment.getWarehouseId(),
                    inventoryTradeDTO, unlockList));
            Response<Boolean> releaseRet = inventoryClient.unLock(releaseList);
            if (!releaseRet.isSuccess() || !releaseRet.getResult()) {
                log.error("fail to unlock inventory, trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, actualShipment, releaseRet.getError());
                createDecreaseStockTask(Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                return Response.fail(releaseRet.getError());
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
                // if (Objects.equals(tradeRet.getError(),"inventory.response.timeout")) {
                    // 超时的错误进入biz表给后面轮询,释放库存biz
                    createShipmentResultTask(Long.parseLong(inventoryTradeDTO.getBizSrcId()));
                // }
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
            currTrade.setBizSrcId(inventoryTradeDTO.getBizSrcId());
            currTrade.setSubBizSrcId(Lists.newArrayList(inventoryTradeDTO.getSubBizSrcId()));
            currTrade.setShopId(inventoryTradeDTO.getShopId());
            currTrade.setUniqueCode(inventoryTradeDTO.getUniqueCode());

            tradeList.add(currTrade);

        }

        return tradeList;
    }


    /**
     * 对于超时这类异常进行后续补偿措施，biz业务轮询
     * @param shipmentId 发货单id
     */
    private void createShipmentResultTask(Long shipmentId){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.STOCK_API_TIME_OUT.toString());
        biz.setContext(mapper.toJson(shipmentId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }


    /**
     * 扣减失败补偿
     * @param shipmentId
     */
    private void createDecreaseStockTask(Long shipmentId){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.STOCK_API_DECREASE_STOCK.toString());
        biz.setContext(mapper.toJson(shipmentId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }


}
