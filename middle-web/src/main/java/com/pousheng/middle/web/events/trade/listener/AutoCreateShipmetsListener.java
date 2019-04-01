/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
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
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onShipment(OpenClientOrderSyncEvent event) {
        log.info("try to auto create shipment,shopOrder id is {}", event.getShopOrderId());
        //使用乐观锁更新操作
        Response<Boolean> updateHandleStatusR = middleOrderWriteService
                .updateHandleStatus(event.getShopOrderId(), String.valueOf(OrderWaitHandleType.WAIT_AUTO_CREATE_SHIPMENT.value()),
                        String.valueOf(OrderWaitHandleType.ORIGIN_STATUS_SAVE.value()));
        if (!updateHandleStatusR.isSuccess()) {
            log.info("update handle status failed,shopOrderId is {},caused by {}", event.getShopOrderId(), updateHandleStatusR.getResult());
            return;
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(event.getShopOrderId());
        log.info("auto create shipment,step one");
        //天猫订单如果还没有拉取售后地址是不能生成发货单的
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.TAOBAO.getValue())) {
            if (shopOrder.getBuyerName().contains("**")) {
                return;
            }
        }
        shipmentWiteLogic.autoHandleOrderForCreateOrder(shopOrder);
    }

}
