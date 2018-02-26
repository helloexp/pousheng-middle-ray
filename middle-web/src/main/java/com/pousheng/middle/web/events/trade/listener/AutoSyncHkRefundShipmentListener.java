package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.web.events.trade.RefundShipmentEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * 售后发货单自动同步恒康
 * Created by tony on 2017/8/23.
 * pousheng-middle
 */
@Slf4j
@Component
public class AutoSyncHkRefundShipmentListener {
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void autoSyncHkRefundShipment(RefundShipmentEvent refundShipmentEvent) {
        Long shipmentId = refundShipmentEvent.getShipmentId();
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        if (Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)){
            log.info("sync shipment to mpos,shipmentId is {}",shipment.getId());
            shipmentWiteLogic.handleSyncShipment(shipment,2,shopOrder);;
        }else{
            Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
            if (!syncRes.isSuccess()) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
            }
        }

    }
}
