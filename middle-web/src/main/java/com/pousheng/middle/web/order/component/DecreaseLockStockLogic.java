package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.manager.WarehouseSkuStockManager;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
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
@Deprecated
public class DecreaseLockStockLogic {
    @RpcConsumer
    private WarehouseSkuStockManager warehouseSkuStockManager;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private DispatchComponent dispatchComponent;

    public void doDecreaseStock(Shipment shipment){
        log.info("start decrease stock,shipmentId is {}",shipment.getId());

        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities =makeSkuCodeAndQuantities(shipmentItems);
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        log.info("will decrease stock,skuCodeAndQuantities is {},warehouseId is {},warehouseName is{}",shipment.getId(),extra.getWarehouseId(),extra.getWarehouseName());
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        Response<Boolean> decreaseStockRlt = warehouseSkuStockManager.decreaseStock(dispatchComponent.genInventoryTradeDTO(shipmentReadLogic.getDispatchOrderItem(shipment)), warehouseShipment, shipmentItems);
        if (!decreaseStockRlt.isSuccess()){
            log.error("this shipment can not decrease stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }

        log.info("end decrease stock");
    }

    private List<SkuCodeAndQuantity> makeSkuCodeAndQuantities(List<ShipmentItem> list){
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        if (list.size()>0){
            for (ShipmentItem shipmentItem:list){
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuOrderId(shipmentItem.getSkuOrderId());
                skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
                skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
                skuCodeAndQuantities.add(skuCodeAndQuantity);
            }
        }
        return skuCodeAndQuantities;
    }

}
