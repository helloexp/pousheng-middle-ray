package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
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
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by tony on 2017/7/13.
 * pousheng-middle
 */
@Slf4j
@Component
public class DecreaseLockStockListener {
    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private StockPusher stockPusher;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void doDecreaseStock(HkShipmentDoneEvent event){

        Shipment shipment = event.getShipment();

        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        List<WarehouseShipment> warehouseShipmentList = Lists.newArrayList();
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities =makeSkuCodeAndQuantities(shipmentItems);
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        warehouseShipmentList.add(warehouseShipment);
        Response<Boolean> decreaseStockRlt =  warehouseSkuWriteService.decreaseStock(warehouseShipmentList,warehouseShipmentList);
        if (!decreaseStockRlt.isSuccess()){
            log.error("this shipment can not decrease stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }
        //触发库存推送
        for (WarehouseShipment ws : warehouseShipmentList) {
            for (SkuCodeAndQuantity skuCodeAndQuantity : ws.getSkuCodeAndQuantities()) {
                stockPusher.submit(skuCodeAndQuantity.getSkuCode());
            }
        }
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
