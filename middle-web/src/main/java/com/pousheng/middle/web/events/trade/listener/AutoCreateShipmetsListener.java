/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * 发货单自动发货的事件
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">zhaoxiaotao</a>
 * Date: 2016-07-31
 */
@Slf4j
@Component
public class AutoCreateShipmetsListener {

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
    public void onShipment(OpenClientOrderSyncEvent event) {
        log.info("try to auto create shipment,shopOrder id is {}",event.getShopOrderId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(event.getShopOrderId());
        log.info("auto create shipment,step one");
        //天猫订单如果还没有拉取售后地址是不能生成发货单的
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())){
            if (shopOrder.getBuyerName().contains("**")){
                return;
            }
        }
        //如果是mpos订单，进行派单
        if(ShopConstants.CHANNEL.equals(shopOrder.getOutFrom())){
            shipmentWiteLogic.toDispatchOrder(shopOrder);
        }else{
            shipmentWiteLogic.doAutoCreateShipment(shopOrder);
        }
    }
}
