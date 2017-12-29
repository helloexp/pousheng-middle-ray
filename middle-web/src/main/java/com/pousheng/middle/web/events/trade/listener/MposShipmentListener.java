package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2017/12/22
 * 发货单事件监听器
 */
@Component
@Slf4j
public class MposShipmentListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteService orderWriteService;



    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    /**
     * 判断是否所有发货单都更新了 更新订单状态
     * @param event
     */
    @Subscribe
    public void onUpdateMposShipment(MposShipmentUpdateEvent event){
        Shipment shipment = shipmentReadLogic.findShipmentById(event.getShipmentId());
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.SHIP){
            this.syncOrderStatus(shipment,MiddleOrderStatus.SHIPPED.getValue());
        }
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.MPOS_REJECT){
            this.reAssignShipment(shipment.getId());
        }
    }


    /**
     * 同步订单状态
     * @param shipment
     * @param targetStatus
     */
    private void syncOrderStatus(Shipment shipment,Integer targetStatus){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long orderShopId = orderShipment.getOrderId();
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
        //获取该订单下所有的orderShipment信息
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(orderShopId);
        //过滤掉已经取消的发货单
        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(it->!Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(),it.getStatus())).collect(Collectors.toList());
        //获取发货单的状态
        List<Integer> orderShipMentStatusList = Lists.transform(orderShipmentsFilter, new Function<OrderShipment, Integer>() {
            @Nullable
            @Override
            public Integer apply(@Nullable OrderShipment orderShipment) {
                return orderShipment.getStatus();
            }
        });
        //判断订单是否已经全部更新状态了
        int count=0;
        for (Integer status:orderShipMentStatusList){
            if (!Objects.equals(status,targetStatus)){
                count++;
            }
        }
        if (count==0) {
            //更新订单状态
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(orderShopId, shopOrder.getStatus());
            for (SkuOrder skuOrder : skuOrders) {
                Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                if (!updateRlt.getResult()) {
                    log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                    throw new JsonResponseException("update.sku.order.status.error");
                }
            }
        }
    }

    private void reAssignShipment(Long shipmentId){

    }
}