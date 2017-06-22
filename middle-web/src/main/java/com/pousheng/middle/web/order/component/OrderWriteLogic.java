package com.pousheng.middle.web.order.component;

import com.google.common.eventbus.EventBus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.web.core.events.trade.OrderCancelEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Mail: F@terminus.io
 * Data: 16/7/19
 * Author: yangzefeng
 */
@Component
@Slf4j
public class OrderWriteLogic {

    @Autowired
    private FlowPicker flowPicker;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @Autowired
    private EventBus eventBus;


    public boolean updateOrder(OrderBase orderBase, OrderLevel orderLevel, OrderEvent orderEvent) {
        Flow flow = flowPicker.pick(orderBase, orderLevel);
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());

        if (Objects.equals(orderEvent.getValue(), OrderEvent.BUYER_CANCEL.getValue())
                ||Objects.equals(orderEvent.getValue(), OrderEvent.SELLER_CANCEL.getValue())) {
            eventBus.post(new OrderCancelEvent(orderBase.getId(), orderLevel.getValue(), orderEvent));
        }

        switch (orderLevel) {
            case SHOP:
                Response<Boolean> updateShopOrderResp = orderWriteService.shopOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateShopOrderResp.isSuccess()) {
                    log.error("fail to update shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus, updateShopOrderResp.getError());
                    throw new JsonResponseException(updateShopOrderResp.getError());
                }
                return updateShopOrderResp.getResult();
            case SKU:
                Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateSkuOrderResp.isSuccess()) {
                    log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus);
                    throw new JsonResponseException(updateSkuOrderResp.getError());
                }
                return updateSkuOrderResp.getResult();
            default:
                throw new IllegalArgumentException("unknown.order.type");
        }
    }
}
