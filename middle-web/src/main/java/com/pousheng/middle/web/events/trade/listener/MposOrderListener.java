package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.events.trade.MposOrderUpdateEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

/**
 * Created by penghui on 2017/12/22
 * mpos订单事件监听器
 */
public class MposOrderListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    /**
     * 订单状态更新
     * @param event
     */
    @Subscribe
    public void onMposOrderConfirmed(MposOrderUpdateEvent event){
       if(Objects.equals(event.getType(),MiddleOrderStatus.CANCEL.getValue())){
           ShopOrder shopOrder = orderReadLogic.findShopOrderById(event.getOrderId());
           //获取该订单下所有的子单和发货单
           List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(event.getOrderId(),
                   MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                   MiddleOrderStatus.WAIT_SHIP.getValue());
           int count = 0;
           List<Shipment> shipments = event.getShipments();
           for (Shipment shipment:shipments){
               if (!shipmentWiteLogic.mposCancelShipment(shipment,0)){
                   //取消失败,后续将整单子单状态设置为取消失败,可以重新发起取消发货单
                   count++;
               }
           }
           if (count>0){
               //发货单取消失败,订单状态设置为取消失败
               middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders, MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation());
           }else {
               //发货单取消成功,订单状态设置为取消成功
               middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
           }
       }
    }


}
