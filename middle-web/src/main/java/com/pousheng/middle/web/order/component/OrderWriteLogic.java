package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.taobao.api.domain.Sku;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/19
 * Author: yangzefeng
 */
@Component
@Slf4j
public class OrderWriteLogic {

    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;


    @Autowired
    private EventBus eventBus;

    public static final Integer BATCH_SIZE = 100;     // 批处理数量


    public boolean updateOrder(OrderBase orderBase, OrderLevel orderLevel, MiddleOrderEvent orderEvent) {
        Flow flow = flowPicker.pickOrder();

        if(!flow.operationAllowed(orderBase.getStatus(),orderEvent.toOrderOperation())){
            log.error("refund(id:{}) current status:{} not allow operation:{}",orderBase.getId(),orderBase.getStatus(),orderEvent.toOrderOperation().getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());

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

    /**
     * 更新子单已处理数量
     * @param skuOrderIdAndQuantity 子单id及数量
     */
    public void updateSkuHandleNumber(Map<Long,Integer> skuOrderIdAndQuantity){

        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Flow flow = flowPicker.pickOrder();

        for (SkuOrder skuOrder : skuOrders){
            //1. 更新extra中剩余待处理数量
            Response<Integer> handleRes = updateSkuOrderExtra(skuOrder,skuOrderIdAndQuantity);
            //2. 判断是否需要更新子单状态
            if(handleRes.isSuccess()){
                Integer targetStatus ;
                //如果剩余数量为0则更新子单状态为待发货
                if(handleRes.getResult()==0){
                    targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.HANDLE_DONE.toOrderOperation());
                }else {
                    targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation());
                }

                Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                if (!updateSkuOrderResp.isSuccess()) {
                    log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                            skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                    throw new ServiceException(updateSkuOrderResp.getError());
                }
            }
        }
    }

    /**
     * 逆向流程:回退子单数量,如果需要则回退子单状态
     * (子单当前的处理状态可能是待发货,处理中,需要将子单状态回滚到处理中或者待处理,店铺订单可能变为处理中或者待处理)
     *
     * @param skuOrderIdAndQuantity 子单id以及数量
     */
    public void updateOrderHandleNumberAndStatus(Map<Long, Integer> skuOrderIdAndQuantity, Shipment shipment) {
        //获取相应的店铺订单
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);

        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long shopOrderId = orderShipment.getOrderId();
        //获取该店铺订单下所有有效的发货单
        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("failed to find order shipment(orderId={}, orderLevel={}), cause:{}", shopOrderId, OrderLevel.SHOP.getValue(), shipmentsRes.getError());
        }
        List<Shipment> shipmentList = shipmentsRes.getResult();
        //过滤掉已经取消的发货单以及当前需要取消的发货单
        List<Shipment> shipmentListFilter = shipmentList.stream().filter(newShipment -> newShipment.getStatus()!= MiddleShipmentsStatus.CANCELED.getValue())
        .filter(newShipment->newShipment.getId()!=shipment.getId()).collect(Collectors.toList());
        //具体的业务处理逻辑
        this.updateOrderStatus4CancelShipment(shipmentListFilter, skuOrders, skuOrderIdAndQuantity);
    }


    /**
     * 取消订单中的子订单
     *
     * @param shopOrderId
     * @param skuCode
     */
    public void cancelSkuOrder(long shopOrderId, String skuCode) {
        //获取店铺订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取sku订单集合,并且需要过滤掉状态为已取消
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrderId);
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull)
                .filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.CANCEL.getValue())).collect(Collectors.toList());
        //根据skuCode查询相应的skuOrder
        SkuOrder skuOrder = this.getSkuOrder(skuOrdersFilter, skuCode);
        //业务校验,判断子单是否有撤单的权限
        if (!valiateSkuOrderStatus4Cancel(shopOrder)) {
            log.error("skuorder can not be canceled,error shopOrder " +
                            "status,skuOrderId is:{},shopOrderId is:{},shopOrder status is:{}"
                    , skuOrder.getId(), shopOrder.getId(), shopOrder.getStatus());
            throw new JsonResponseException("cancel.sku.order.error");
        }
        if (!valiateSkuOrderStatus4Cancel(skuOrder)) {
            log.error("skuorder can not be canceled,error shopOrder " +
                            "status,skuOrderId is:{},shopOrderId is:{},skuorder status is:{}"
                    , skuOrder.getId(), shopOrder.getId(), skuOrder.getStatus());
            throw new JsonResponseException("cancel.sku.order.error");
        }

        //该子订单没有生成发货单,取消该子订单,更新子订单的状态为已取消
        if (skuOrder.getStatus()==MiddleOrderStatus.WAIT_HANDLE.getValue()) {
            //如果店铺订单只有一条有效子订单,需要更新店铺订单的状态为已取消
            this.updateOrderAndSkuOrderToCanceled(skuOrdersFilter, skuOrder);
        } else {
            //获取所有有效的发货单
            Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
            if (!shipmentsRes.isSuccess()) {
                log.error("failed to find order shipment(orderId={}, orderLevel={}), cause:{}", shopOrderId, OrderLevel.SHOP.getValue(), shipmentsRes.getError());
            }
            List<Shipment> shipmentList = shipmentsRes.getResult();
            //过滤掉已经取消的发货单
            List<Shipment> shipmentListFilter = shipmentList.stream().filter(newShipment ->
                    (newShipment.getStatus()!=MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
            for (Shipment s : shipmentListFilter) {
                //判断发货单是否包含该子订单,如果不包含则继续下一循环
                List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(s);
                if (items.stream().filter(shipmentItem -> (shipmentItem.getSkuOrderId() == skuOrder.getId())).collect(Collectors.toList()).size() == 0) {
                    continue;
                }
                //将发货单状态修改为已取消
                if (s.getStatus() == MiddleShipmentsStatus.WAIT_SHIP.getValue()) {
                    //todo 同步取消通知恒康
                    Response<Boolean> cancelRes = syncShipmentLogic.syncShipmentCancelToHk(s);
                    if (!cancelRes.isSuccess()) {
                        log.error("cancel shipment(id:{}) fail,error:{}", s.getId(), cancelRes.getError());
                        throw new JsonResponseException(cancelRes.getError());
                    }
                } else {
                    Response<Boolean> cancelRes = shipmentWiteLogic.updateStatus(s, MiddleOrderEvent.CANCEL.toOrderOperation());
                    if (!cancelRes.isSuccess()) {
                        log.error("cancel shipment(id:{}) fail,error:{}", s.getId(), cancelRes.getError());
                        throw new JsonResponseException(cancelRes.getError());
                    }
                }
                //该发货单存在需要取消的子订单,但是不存在其他的子订单
                List<ShipmentItem> itemsFilter = items.stream().filter(shipmentItem -> (shipmentItem.getSkuOrderId() != skuOrder.getId())).collect(Collectors.toList());
                if (itemsFilter.size() == 0) {
                    continue;
                }
                //将该发货单下其他子订单的待处理数量回滚,状态回滚
                Map<Long, Integer> skuOrderIdAndQuantityMap = itemsFilter.stream().filter(Objects::nonNull)
                        .collect(Collectors.toMap(ShipmentItem::getSkuOrderId, ShipmentItem::getQuantity));
                this.updateOrderHandleNumberAndStatus(skuOrderIdAndQuantityMap, s);

                //将取消的这个子单的数量回滚,状态不变
                List<ShipmentItem> itemsCanceled = items.stream().filter(shipmentItem -> (shipmentItem.getSkuOrderId()==skuOrder.getId())).collect(Collectors.toList());
                Map<Long, Integer> skuOrderIdAndQuantityCanceled = itemsCanceled.stream().filter(Objects::nonNull)
                        .collect(Collectors.toMap(ShipmentItem::getSkuOrderId, ShipmentItem::getQuantity));
                this.rollbackSkuOrderExtra(skuOrder,skuOrderIdAndQuantityCanceled);

            }
            //将需要撤单的子订单或者店铺订单状态更新为已取消
            this.updateOrderAndSkuOrderToCanceled(skuOrdersFilter, skuOrder);
        }
    }

    private Response<Integer> updateSkuOrderExtra(SkuOrder skuOrder,Map<Long,Integer> skuOrderIdAndQuantity){
        Map<String,String> extraMap = skuOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("sku order(id:{}) extra is null,can not update wait handle number reduce：{}",skuOrder.getId(),skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("sku.extra.field.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.WAIT_HANDLE_NUMBER)){
            log.error("sku order(id:{}) extra not contains key:{},can not update wait handle number reduce：{}",skuOrder.getId(),TradeConstants.WAIT_HANDLE_NUMBER,skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("sku.extra.not.contains.key.wait.handle.number");
        }
        Integer waitHandleNumber = Integer.valueOf(extraMap.get(TradeConstants.WAIT_HANDLE_NUMBER));
        if(waitHandleNumber<=0){
            log.error("sku order(id:{}) extra wait handle number:{} ,not enough to ship",skuOrder.getId(),waitHandleNumber);
            return Response.fail("sku.order.wait.handle.number.invalid");
        }
        Integer quantity = skuOrderIdAndQuantity.get(skuOrder.getId());
        Integer remainNumber = waitHandleNumber - quantity;
        if(remainNumber<0){
            log.error("sku order(id:{}) extra wait handle number:{} ship applyQuantity:{} ,not enough to ship",skuOrder.getId(),waitHandleNumber,quantity);
            return Response.fail("handle.number.get.wait.handle.number");
        }
        extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER,String.valueOf(remainNumber));
        Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,extraMap);
        if(!response.isSuccess()){
            log.error("update sku order：{} extra map to:{} fail,error:{}",skuOrder.getId(),extraMap,response.getError());
            return Response.fail(response.getError());
        }

        return Response.ok(remainNumber);
    }

    /**
     * 回滚sku订单待处理数量
     * @param skuOrder
     * @param skuOrderIdAndQuantity
     * @return
     */
    private Response<Integer> rollbackSkuOrderExtra(SkuOrder skuOrder, Map<Long, Integer> skuOrderIdAndQuantity) {
        Map<String, String> extraMap = skuOrder.getExtra();
        if (CollectionUtils.isEmpty(extraMap)) {
            log.error("sku order(id:{}) extra is null,can not update wait handle number reduce：{}", skuOrder.getId(), skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("sku.extra.field.is.null");
        }
        if (!extraMap.containsKey(TradeConstants.WAIT_HANDLE_NUMBER)) {
            log.error("sku order(id:{}) extra not contains key:{},can not update wait handle number reduce：{}", skuOrder.getId(), TradeConstants.WAIT_HANDLE_NUMBER, skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("sku.extra.not.contains.key.wait.handle.number");
        }
        Integer waitHandleNumber = Integer.valueOf(extraMap.get(TradeConstants.WAIT_HANDLE_NUMBER));
        if (waitHandleNumber < 0) {
            log.error("sku order(id:{}) extra wait handle number:{} ,not enough to ship", skuOrder.getId(), waitHandleNumber);
            return Response.fail("sku.order.wait.handle.number.invalid");
        }
        Integer quantity = skuOrderIdAndQuantity.get(skuOrder.getId());
        Integer remainNumber = waitHandleNumber + quantity;
        if (remainNumber < 0) {
            log.error("sku order(id:{}) extra wait handle number:{} ship applyQuantity:{} ,not enough to ship", skuOrder.getId(), waitHandleNumber, quantity);
            return Response.fail("handle.number.get.wait.handle.number");
        }
        extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(remainNumber));
        Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(), OrderLevel.SKU, extraMap);
        if (!response.isSuccess()) {
            log.error("update sku order：{} extra map to:{} fail,error:{}", skuOrder.getId(), extraMap, response.getError());
            return Response.fail(response.getError());
        }

        return Response.ok(remainNumber);
    }
    /**
     * 根据skuCode获取List<skuOrder>集合中对应的skuOrder的记录
     *
     * @param skuOrders
     * @param skuCode
     * @return
     */
    private SkuOrder getSkuOrder(List<SkuOrder> skuOrders, String skuCode) {
        //确保订单非空并且订单处于非取消状态
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).filter(skuOrder -> (skuOrder.getStatus()!=MiddleOrderStatus.CANCEL.getValue()))
                .filter(skuOrder -> (Objects.equals(skuCode, skuOrder.getSkuCode()))).collect(Collectors.toList());
        if (skuOrdersFilter.size() == 0) {
            log.error("fail to find sku order by skuCode:{}", skuCode);
            throw new JsonResponseException("sku.order.not.exist");
        }
        return skuOrdersFilter.get(0);

    }

    //订单处于待处理,处理中,待发货则可以退货(可以校验sku订单,店铺订单)
    private boolean valiateSkuOrderStatus4Cancel(OrderBase orderBase) {
        if ((orderBase.getStatus()== MiddleOrderStatus.WAIT_HANDLE.getValue())||
                (orderBase.getStatus()==MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue()) ||
                (orderBase.getStatus()==MiddleOrderStatus.WAIT_SHIP.getValue())) {
            return true;
        }
        return false;
    }

    /**
     * 取消发货单时回滚待处理数量和更新订单状态的业务逻辑处理
     *
     * @param shipmentListFilter
     * @param skuOrders
     * @param skuOrderIdAndQuantity
     */
    private void updateOrderStatus4CancelShipment(List<Shipment> shipmentListFilter, List<SkuOrder> skuOrders, Map<Long, Integer> skuOrderIdAndQuantity) {
        Flow flow = flowPicker.pickOrder();
        for (SkuOrder skuOrder : skuOrders) {
            //1. 回滚extra中剩余待处理数量
            Response<Integer> handleRes = rollbackSkuOrderExtra(skuOrder, skuOrderIdAndQuantity);
            //2. 判断是否需要更新子单状态
            if (handleRes.isSuccess()) {
                Integer targetStatus;
                //判断该子单是否在其他其他的发货单中是否存在
                Boolean isSkuOrderExist = false;
                for (Shipment s : shipmentListFilter) {
                    List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(s);
                    //判断该发货单中是否存在子单
                    if (items.stream().filter(shipmentItem -> shipmentItem.getSkuOrderId()==skuOrder.getId()).collect(Collectors.toList()).size() > 0) {
                        isSkuOrderExist = true;
                        break;
                    }
                }
                //如果该子订单和其他发货单存在关联
                if (isSkuOrderExist) {
                    //如果sku订单处于待发货状态,此时才修改状态
                    if (Objects.equals(skuOrder.getStatus(), MiddleOrderStatus.WAIT_SHIP.getValue())) {
                        targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.SHIP_CANCEL.toOrderOperation());
                        Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                        if (!updateSkuOrderResp.isSuccess()) {
                            log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                                    skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                            throw new ServiceException(updateSkuOrderResp.getError());
                        }
                    }
                } else {
                    //该子单和其他发货单没有关联
                    targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.SHIP_CANCEL_DONE.toOrderOperation());
                    Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                    if (!updateSkuOrderResp.isSuccess()) {
                        log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                                skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                        throw new ServiceException(updateSkuOrderResp.getError());
                    }
                }

            }
        }
    }

    /**
     * 更新skuOrder或者ShopOrder的状态为已取消
     * 子单只有一条记录,则同时更新子单和总单
     * 子单不止一条记录,则更新子单
     *
     * @param skuOrders
     * @param skuOrder
     */
    private void updateOrderAndSkuOrderToCanceled(List<SkuOrder> skuOrders, SkuOrder skuOrder) {
        Flow flow = flowPicker.pickOrder();
        if (skuOrders.size() <= 1) {
            Integer targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.CANCEL.toOrderOperation());
            Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
            if (!updateSkuOrderResp.isSuccess()) {
                log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                        skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                throw new ServiceException(updateSkuOrderResp.getError());
            }
        } else {
            Integer targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.CANCEL.toOrderOperation());
            Response<Boolean> updateSkuOrderResp = orderWriteService.updateOrderStatus(skuOrder.getId(), OrderLevel.SKU, targetStatus);
            if (!updateSkuOrderResp.isSuccess()) {
                log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                        skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                throw new ServiceException(updateSkuOrderResp.getError());
            }
        };
    }
}

