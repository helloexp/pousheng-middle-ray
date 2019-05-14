/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.events.trade.RefundShipmentEvent;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.SkuCodeUtil;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 更新换货商品已处理数量
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-05-23
 */
@Slf4j
@Component
public class RefundShipmentListener {


    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onRefundShipment(RefundShipmentEvent refundShipmentEvent) {
        Long shipmentId = refundShipmentEvent.getShipmentId();
        log.info("EVEN-BUS-RefundShipmentListener handle start shipment id:{}",shipmentId);

        Shipment shipment  = shipmentReadLogic.findShipmentById(shipmentId);
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        //Map<String, Integer> skuCodeAndQuantityMap = shipmentItems.stream().filter(Objects::nonNull)
        //        .collect(Collectors.toMap(ShipmentItem::getSkuCode, ShipmentItem::getQuantity));
        Map<String, Integer> skuCodeAndQuantityMap = shipmentItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(it -> SkuCodeUtil.getCombineCode(it),ShipmentItem::getQuantity));
        Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
            refundWriteLogic.updateSkuHandleNumber(orderShipment.getAfterSaleOrderId(),skuCodeAndQuantityMap);
        }else{
            refundWriteLogic.updateSkuHandleNumberForLost(orderShipment.getAfterSaleOrderId(),skuCodeAndQuantityMap);
        }

        log.info("EVEN-BUS-RefundShipmentListener handle end shipment id:{}",shipmentId);


    }
}
