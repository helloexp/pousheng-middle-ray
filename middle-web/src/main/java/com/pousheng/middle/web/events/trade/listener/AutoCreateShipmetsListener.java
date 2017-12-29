/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

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
        //如果是mpos订单，进行派单
        if(ShopConstants.CHANNEL.equals(shopOrder.getOutFrom())){
            shipmentWiteLogic.toDispatchOrder(shopOrder);
        }else{
            shipmentWiteLogic.doAutoCreateShipment(shopOrder);
        }
    }
}
