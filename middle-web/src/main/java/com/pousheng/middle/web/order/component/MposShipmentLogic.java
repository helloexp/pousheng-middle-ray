package com.pousheng.middle.web.order.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/2/25
 * pousheng-middle
 */
@Component
@Slf4j
public class MposShipmentLogic {
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private SyncShipmentLogic syncShipmentLogic;

    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    /**
     * 判断是否所有发货单都更新了 更新订单状态
     * @param event
     */
    public void onUpdateMposShipment(MposShipmentUpdateEvent event){
        log.info("start to update order status,when mops shipped,event param{}",event.getShipmentId());
        Shipment shipment = shipmentReadLogic.findShipmentById(event.getShipmentId());
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.SHIP){
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if(Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)){
                //扣减库存
                mposSkuStockLogic.decreaseStock(shipment);
                // 发货推送pos信息给恒康
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                if(!response.isSuccess()){
                    Map<String,Object> param = Maps.newHashMap();
                    param.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
                }
            }
            this.syncOrderStatus(shipment, MiddleOrderStatus.SHIPPED.getValue());
        }
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.MPOS_REJECT){
            //解锁库存
            mposSkuStockLogic.unLockStock(shipment);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            List<SkuCodeAndQuantity> skuCodeAndQuantities = shipmentReadLogic.findShipmentSkuDetail(shipment);
            shipmentWiteLogic.toDispatchOrder(shopOrder,skuCodeAndQuantities);
        }
        log.info("end to update order status,when mops shipped");
    }

    /**
     * 同步订单状态
     * @param shipment      发货单
     * @param expectOrderStatus   期望订单状态
     */
    private void syncOrderStatus(Shipment shipment,Integer expectOrderStatus){

        //更新子单状态
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        List<Long> skuOrderIds = shipmentItems.stream().map(ShipmentItem::getSkuOrderId).collect(Collectors.toList());
        List<SkuOrder> skuOrderList = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        for (SkuOrder skuOrder : skuOrderList) {
            Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), expectOrderStatus);
            if (!updateRlt.getResult()) {
                log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                throw new JsonResponseException("update.sku.order.status.error");
            }
        }

        //如果订单状态变成已发货，同步ecpstatus
        if(Objects.equals(expectOrderStatus,MiddleOrderStatus.SHIPPED.getValue())){
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            if(Objects.equals(shopOrder.getStatus(),MiddleOrderStatus.SHIPPED.getValue())){
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            }
        }
    }
}
