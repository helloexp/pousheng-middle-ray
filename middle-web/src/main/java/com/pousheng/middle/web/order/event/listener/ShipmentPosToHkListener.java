/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.order.event.listener;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
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
    @Autowired
    private CompensateBizLogic compensateBizLogic;

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
        //生成发货单同步恒康生成pos的任务
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizId(String.valueOf(shipment.getId()));
        biz.setBizType(PoushengCompensateBizType.SYNC_ORDER_POS_TO_HK.name());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);

    }

}
