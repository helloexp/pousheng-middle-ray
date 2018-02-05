package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
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
import java.util.stream.Collectors;

/**
 *发货单发货完成后通知电商的方法
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/4
 * pousheng-middle
 */
@Slf4j
@Component
public class HKShipmentDoneLogic {
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @Autowired
    private EcpOrderLogic ecpOrderLogic;
    @Autowired
    private DecreaseLockStockLogic decreaseLockStockLogic;

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
    private SyncMposShipmentLogic syncMposShipmentLogic;

    public void doneShipment(Shipment shipment) {
        log.info("HK SHIPMENT DONE LISTENER start, shipmentId is {},shipmentType is {}",shipment.getId(),shipment.getType());
        //判断发货单是否发货完
        if (shipment.getType() == ShipmentType.SALES_SHIP.value()) {
            //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            long orderShopId = orderShipment.getOrderId();
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
            Flow flow = flowPicker.pickOrder();
            log.info("HK SHIPMENT DONE LISTENER shopOrderStatus is {}", shopOrder.getStatus());
            if (flow.operationAllowed(shopOrder.getStatus(), MiddleOrderEvent.SHIP.toOrderOperation())) {
                //更新子订单中的信息
                List<Long> skuOrderIds = Lists.newArrayList();
                Map<Long, Integer> skuInfos = shipment.getSkuInfos();
                skuOrderIds.addAll(skuInfos.keySet());
                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
                for (SkuOrder skuOrder : skuOrders) {
                    Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), MiddleOrderStatus.SHIPPED.getValue());
                    if (!updateRlt.getResult()) {
                        log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                        throw new JsonResponseException("update.sku.order.status.error");
                    }
                }
            }
            log.info("wait to notify ecp,shipmentId is {},shipmentType is {}", shipment.getId(), shipment.getType());
                //尝试同步发货信息到电商平台,如果有多个发货单，需要等到所有的发货单发货完成之后才会通知电商平台
                ecpOrderLogic.shipToEcp(shipment.getId());
        }

        //丢件补发类型的发货单的类型是3，中台没有相应的枚举类
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
                    Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.SHIP.toOrderOperation());
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

        //真正扣减库存
        decreaseLockStockLogic.doDecreaseStock(shipment);

        log.info("HK SHIPMENT DONE LISTENER end, shipmentId is {},shipmentType is {}",shipment.getId(),shipment.getType());

    }
}
