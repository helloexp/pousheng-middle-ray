package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.events.trade.OrderShipmentEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 自动创建发货单同步恒康事件
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncHkShipmentListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void syncHkShipment(OrderShipmentEvent orderShipmentEvent) {
        long shipmentId = orderShipmentEvent.getShipmentId();
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
        if(!syncRes.isSuccess()){
            log.error("sync shipment(id:{}) to hk fail,error:{}",shipmentId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }
}
