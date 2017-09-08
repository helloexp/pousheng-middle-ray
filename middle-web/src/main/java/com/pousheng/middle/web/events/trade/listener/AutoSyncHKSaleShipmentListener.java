package com.pousheng.middle.web.events.trade.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 销售发货单自动同步恒康
 * Created by tony on 2017/8/23.
 * pousheng-middle
 */
@Slf4j
@Component
public class AutoSyncHKSaleShipmentListener {
  /*  @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void autoSyncHkSaleShipment(OrderShipmentEvent orderShipmentEvent) {
       *//* Long shipmentId = orderShipmentEvent.getShipmentId();
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
        if (!syncRes.isSuccess()) {
            log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
        }*//*
    }*/
}
