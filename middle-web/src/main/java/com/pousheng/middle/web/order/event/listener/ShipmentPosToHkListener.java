/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.order.event.listener;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.HKShipmentDoneLogic;
import com.pousheng.middle.web.order.event.ShipmentPosToHkEvent;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 发货单自动发货的事件
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">zhaoxiaotao</a>
 * Date: 2016-07-31
 */
@Slf4j
@Component
public class ShipmentPosToHkListener {

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onShipment(ShipmentPosToHkEvent event) {
        log.info("try to sync shipment(id:{}) to hk",event.getShipment().getId());
        Shipment shipment = event.getShipment();

        //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
        hkShipmentDoneLogic.doneShipment(shipment);

        //同步pos单到恒康
        Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
        if (!response.isSuccess()) {
            Map<String, Object> param1 = Maps.newHashMap();
            param1.put("shipmentId", shipment.getId());
            autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
        }

    }

}
