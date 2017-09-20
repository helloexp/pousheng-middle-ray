/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 更新子单商品待处理数量
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
@Slf4j
@Component
public class OrderShipmentListener {

  /*  @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onShipment(OrderShipmentEvent orderShipmentEvent) {
        Long shipmentId = orderShipmentEvent.getShipmentId();
        Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
        if (!shipmentRes.isSuccess()) {
            log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
            return;
        }

        orderWriteLogic.updateSkuHandleNumber(shipmentRes.getResult().getSkuInfos());

    }*/
}
