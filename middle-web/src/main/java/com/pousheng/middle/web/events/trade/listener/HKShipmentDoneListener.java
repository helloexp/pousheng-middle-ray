package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.*;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 一旦订单或者售后单下面的发货单已经全部发货,更新订单或者发货单的状态为已经发货
 * Created by tony on 2017/7/10.
 * pousheng-middle
 */
@Slf4j
@Component
public class HKShipmentDoneListener {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
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
    private WarehouseSkuWriteService warehouseSkuWriteService;
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
        if (shipment.getType()==ShipmentType.SALES_SHIP.value()) {
            //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            if (shopOrder.getStatus()== MiddleOrderStatus.WAIT_SHIP.getValue()) {
                List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(orderShopId);
                List<Integer> orderShipMentStatusList = Lists.transform(orderShipments, new Function<OrderShipment, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable OrderShipment orderShipment) {
                        return orderShipment.getStatus();
                    }
                });

                //判断此时是否还有处于待发货的发货单,如果没有,则此时店铺订单状态应该更新为待通知电商平台,sku订单更新状态时应该考虑排除已取消订单状态的更新
             if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                    //待发货--商家已经发货
                    List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
                    List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).
                            filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.CANCEL.getValue())).collect(Collectors.toList());
                    for (SkuOrder skuOrder:skuOrdersFilter){
                        Response<Boolean> updateRlt = orderWriteService.updateOrderStatus(skuOrder.getId(),OrderLevel.SKU,MiddleOrderStatus.SHIPPED.getValue());
                        if (!updateRlt.getResult()) {
                            log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                            throw new JsonResponseException("update.sku.order.status.error");
                        }
                    }
                        //更新总的订单
                     Response<Boolean> updateRlt = orderWriteService.updateOrderStatus(shopOrder.getId(),OrderLevel.SHOP,MiddleOrderStatus.SHIPPED.getValue());
                     if (!updateRlt.getResult()) {
                         log.error("update shopOrder status error (id:{}),original status is {}", shopOrder.getId(), shopOrder.getStatus());
                         throw new JsonResponseException("update.shop.order.status.error");
                     }
                }
            }
        }
        if (shipment.getType()==ShipmentType.EXCHANGE_SHIP.value()) {
            //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
            Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
            if (refund.getStatus()==MiddleRefundStatus.WAIT_SHIP.getValue()) {
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
        //扣减库存
        this.decreaseStock(shipment);
    }

    /**
     * 扣减库存方法
     * @param shipment
     */
    private void decreaseStock(Shipment shipment){
        //扣减库存
        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        List<WarehouseShipment> warehouseShipmentList = Lists.newArrayList();
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.transform(shipmentItems, new Function<ShipmentItem, SkuCodeAndQuantity>() {
            @Nullable
            @Override
            public SkuCodeAndQuantity apply(@Nullable ShipmentItem shipmentItem) {
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
                skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
                return skuCodeAndQuantity;
            }
        });
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        warehouseShipmentList.add(warehouseShipment);
        Response<Boolean> decreaseStockRlt =  warehouseSkuWriteService.decreaseStock(warehouseShipmentList,warehouseShipmentList);
        if (!decreaseStockRlt.isSuccess()){
            log.error("this shipment can not unlock stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
            throw new JsonResponseException("decrease.stock.error");
        }
    }
}
