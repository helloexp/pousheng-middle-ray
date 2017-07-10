package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一旦订单或者售后单下面的发货单已经全部发货,更新订单或者发货单的状态为已经发货
 * Created by tony on 2017/7/10.
 * pousheng-middle
 */
@Slf4j
public class HKShipmentDoneListener {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteService refundWriteService;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void doneShipment(HkShipmentDoneEvent event){
        Shipment shipment = event.getShipment();
        //判断发货单是否发货完
        if (Objects.equals(shipment.getType(), ShipmentType.SALES_SHIP.value())) {
            //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.WAIT_SHIP.getValue())) {
                Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByOrderIdAndOrderLevel(orderShopId, OrderLevel.SHOP);
                List<Integer> orderShipMentStatusList = Lists.transform(listResponse.getResult(), new Function<OrderShipment, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable OrderShipment orderShipment) {
                        return orderShipment.getStatus();
                    }
                });
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                    //待发货--商家已经发货
                    boolean updateRlt = orderWriteLogic.updateOrder(shopOrder, OrderLevel.SHOP, MiddleOrderEvent.SHIP);
                    if (!updateRlt) {
                        log.error("update shopOrder status error (id:{}),original status is {}", shopOrder.getId(), shopOrder.getStatus());
                        throw new JsonResponseException("update.shop.order.status.error");
                    }
                }
            }
        }
        if (Objects.equals(shipment.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
            //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
            Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
            if (Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_SHIP.getValue())) {
                Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
                List<Integer> orderShipMentStatusList = Lists.transform(listResponse.getResult(), new Function<OrderShipment, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable OrderShipment orderShipment) {
                        return orderShipment.getStatus();
                    }
                });
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                    //更新售后单的处理状态

                    Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.SHIP.toOrderOperation());
                    if (!resRlt.isSuccess()) {
                        log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                        throw new JsonResponseException("update.refund.status.error");
                    }
                    //将shipmentExtra的已发货时间塞入值
                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                    refundExtra.setShipAt(new Date());
                    Map<String, String> extrMap = refund.getExtra();
                    extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    refund.setExtra(extrMap);
                    Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                    if (!updateRefundRes.isSuccess()) {
                        log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                        throw new JsonResponseException("update.refund.error");
                    }
                }
            }
        }
    }

}
