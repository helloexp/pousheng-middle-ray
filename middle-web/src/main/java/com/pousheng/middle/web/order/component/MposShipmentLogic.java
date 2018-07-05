package com.pousheng.middle.web.order.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/2/25
 * pousheng-middle
 */
@Component
@Slf4j
public class MposShipmentLogic {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private SyncShipmentLogic syncShipmentLogic;

    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;
    @Autowired
    private EcpOrderLogic ecpOrderLogic;
    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundWriteService refundWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    /**
     * 判断是否所有发货单都更新了 更新订单状态
     *
     * @param event
     */
    public void onUpdateMposShipment(MposShipmentUpdateEvent event) {
        log.info("start to update order status,when mops shipped,event param{}", event.getShipmentId());
        Shipment shipment = shipmentReadLogic.findShipmentById(event.getShipmentId());
        if (event.getMiddleOrderEvent() == MiddleOrderEvent.SHIP) {
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)) {
                //扣减库存
                mposSkuStockLogic.decreaseStock(shipment);
                // 发货推送pos信息给恒康
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                if (!response.isSuccess()) {
                    Map<String, Object> param = Maps.newHashMap();
                    param.put("shipmentId", shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param, TradeConstants.FAIL_SYNC_POS_TO_HK, response.getError());
                }
            }
            this.syncOrderStatus(shipment, MiddleOrderStatus.SHIPPED.getValue());
        }
        if (event.getMiddleOrderEvent() == MiddleOrderEvent.MPOS_REJECT) {
            //解锁库存
            mposSkuStockLogic.unLockStock(shipment);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            List<SkuCodeAndQuantity> skuCodeAndQuantities = shipmentReadLogic.findShipmentSkuDetail(shipment);
            shipmentWiteLogic.toDispatchOrder(shopOrder, skuCodeAndQuantities);
        }
        log.info("end to update order status,when mops shipped shipment id:{}",event.getShipmentId());
    }

    /**
     * 同步订单状态
     *
     * @param shipment          发货单
     * @param expectOrderStatus 期望订单状态
     */
    public void syncOrderStatus(Shipment shipment, Integer expectOrderStatus) {
        log.info("sync mpos order status start,shipment is {},expected order status is {}", shipment, expectOrderStatus);
        //更新子单状态
        log.info("start sync mpos,all channel,new allchannel order sync to ecp start.....");
        //如果订单状态变成已发货，同步ecpstatus
        if (shipment.getType() == ShipmentType.SALES_SHIP.value()) {
            //订单修改
            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<Long> skuOrderIds = shipmentItems.stream().map(ShipmentItem::getSkuOrderId).collect(Collectors.toList());
            List<SkuOrder> skuOrderList = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
            for (SkuOrder skuOrder : skuOrderList) {
                Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), expectOrderStatus);
                if (!updateRlt.getResult()) {
                    log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                    throw new JsonResponseException("update.sku.order.status.error");
                }
            }

            if (!orderReadLogic.isNewAllChannelOpenShop(shipment.getShopId())) {
                log.info("mpos order notify ecp start,shipment is {}", shipment);
                if (Objects.equals(expectOrderStatus, MiddleOrderStatus.SHIPPED.getValue())) {
                    OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
                    long orderShopId = orderShipment.getOrderId();
                    ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
                    if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.SHIPPED.getValue())) {
                        OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                        orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                    }
                }
            } else {
                log.info("new all channel  order notify ecp start,shipment is {}", shipment);
                ecpOrderLogic.shipToEcp(shipment.getId());
            }
        }

        if (shipment.getType() == ShipmentType.EXCHANGE_SHIP.value()||shipment.getType()==3) {
            //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成,C
            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
            OrderShipment orderShipment = orderShipmentResponse.getResult();
            long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
            List<OrderShipment> orderShipments = shipmentReadLogic.findByAfterOrderIdAndType(afterSaleOrderId);
            //获取该售后单下所有的发
            Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
            List<Integer> shipmentStatuses = orderShipments.stream().map(OrderShipment::getStatus).collect(Collectors.toList());
            if (shipmentStatuses.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())){
                return;
            }
            if (refund.getStatus() == MiddleRefundStatus.WAIT_SHIP.getValue()) {
                Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
                List<Integer> orderShipMentStatusList = listResponse.getResult().stream().map(OrderShipment::getStatus).collect(Collectors.toList());
                log.info("HK SHIPMENT DONE LISTENER for change,orderShipMentStatusList is {}",orderShipMentStatusList);
                if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())
                        && !orderShipMentStatusList.contains(MiddleShipmentsStatus.SYNC_HK_ING.getValue()) &&
                        !orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue())) {
                    Response<Boolean> resRlt = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.SHIP.toOrderOperation());
                    if (!resRlt.isSuccess()) {
                        log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                        throw new JsonResponseException("update.refund.status.error");
                    }
                    //将shipmentExtra的已发货时间塞入值,
                    Flow flow = flowPicker.pickAfterSales();
                    Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.SHIP.toOrderOperation());
                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                    refundExtra.setShipAt(new Date());
                    Map<String, String> extrMap = refund.getExtra();
                    extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    refund.setExtra(extrMap);
                    refund.setStatus(targetStatus);
                    Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                    if (!updateRefundRes.isSuccess()) {
                        log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                        throw new JsonResponseException("update.refund.error");
                    }
                }
            }
            //丢件补发类型
            if (refund.getStatus() == MiddleRefundStatus.LOST_WAIT_SHIP.getValue()) {
                log.info("HK SHIPMENT DONE LISTENER for lost,shipmentId is {}",shipment.getId());
                Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.LOST_SHIPPED.toOrderOperation());
                if (!resRlt.isSuccess()) {
                    log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                    throw new JsonResponseException("update.refund.status.error");
                }
                //将shipmentExtra的已发货时间塞入值,
                Flow flow = flowPicker.pickAfterSales();
                Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.LOST_SHIPPED.toOrderOperation());
                RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                refundExtra.setShipAt(new Date());
                Map<String, String> extrMap = refund.getExtra();
                extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                refund.setExtra(extrMap);
                refund.setStatus(targetStatus);
                Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                if (!updateRefundRes.isSuccess()) {
                    log.error("update refund(id:{}) fail,error:{}", refund, updateRefundRes.getError());
                    throw new JsonResponseException("update.refund.error");
                }
            }
        }
    }
}
