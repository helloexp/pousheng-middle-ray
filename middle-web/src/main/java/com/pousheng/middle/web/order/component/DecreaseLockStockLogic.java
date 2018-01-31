package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 扣减库存同步方法
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/4
 * pousheng-middle
 */
@Slf4j
@Component
public class DecreaseLockStockLogic {
    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private StockPusher stockPusher;

    public void doDecreaseStock(Shipment shipment){
        log.info("start decrease stock,shipmentId is {}",shipment.getId());
        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        List<WarehouseShipment> warehouseShipmentList = Lists.newArrayList();
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities =makeSkuCodeAndQuantities(shipmentItems);
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        log.info("will decrease stock,skuCodeAndQuantities is {},warehouseId is {},warehouseName is{}",shipment.getId(),extra.getWarehouseId(),extra.getWarehouseName());
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        warehouseShipmentList.add(warehouseShipment);
        Response<Boolean> decreaseStockRlt =  warehouseSkuWriteService.decreaseStock(warehouseShipmentList,warehouseShipmentList);
        if (!decreaseStockRlt.isSuccess()){
            log.error("this shipment can not decrease stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }
        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        for (WarehouseShipment ws : warehouseShipmentList) {
            for (SkuCodeAndQuantity skuCodeAndQuantity : ws.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }
        stockPusher.submit(skuCodes);
        log.info("end decrease stock");
    }

    private List<SkuCodeAndQuantity> makeSkuCodeAndQuantities(List<ShipmentItem> list){
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        if (list.size()>0){
            for (ShipmentItem shipmentItem:list){
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
                skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
                skuCodeAndQuantities.add(skuCodeAndQuantity);
            }
        }
        return skuCodeAndQuantities;
    }
}