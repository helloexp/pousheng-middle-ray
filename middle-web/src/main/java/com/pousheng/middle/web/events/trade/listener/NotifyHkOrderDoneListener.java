package com.pousheng.middle.web.events.trade.listener;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
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
            Long shipmentId = orderShipment.getShipmentId();
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            //如果是店发，通知给hk（针对mpos订单）
            if(Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)){
                Response<Boolean> res = shipmentWiteLogic.updateStatus(shipment,MiddleOrderEvent.HK_CONFIRMD_SUCCESS.toOrderOperation());
                if(!res.isSuccess()){
                    log.error("shipment(id:{}) confirm failed,cause:{}",shipment.getId(),res.getError());
                }
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentDoneToHk(shipment);
                if(!response.isSuccess()){
                    log.error("shipment(id:{}) notify hk failed,cause:{}",response.getError());
                    Map<String,Object> param = Maps.newHashMap();
                    param.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK);
                }
                continue ;
            }
            //通知恒康已经发货
            Response<Boolean> response= syncShipmentLogic.syncShipmentDoneToHk(shipment,2, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
            if (!response.isSuccess()){
                log.error("notify hk order confirm failed,shipment id is ({}),caused by {}",shipment.getId(),response.getError());
            }
        }
    }
}
