package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知恒康电商平台已经收货的事件
 * Created by tony on 2017/8/25.
 * pousheng-middle
 */
@Slf4j
@Component
public class NotifyHkOrderDoneListener {
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
    public void notifyHkOrderConfirmed(NotifyHkOrderDoneEvent event) {
        Long shopOrderId = event.getShopOrderId();
        List<OrderShipment> orderShipments =  shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        //获取已发货的发货单
        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> Objects.equals(orderShipment.getStatus(),MiddleShipmentsStatus.SHIPPED.getValue()))
                .collect(Collectors.toList());
        for (OrderShipment orderShipment:orderShipmentsFilter){
            //通知恒康已经发货
            Long shipmentId = orderShipment.getShipmentId();
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            Response<Boolean> response= syncShipmentLogic.syncShipmentDoneToHk(shipment,2, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
            if (!response.isSuccess()){
                log.error("notify hk order confirm failed,shipment id is ({}),caused by {}",shipment.getId(),response.getError());
            }
        }
    }
}
