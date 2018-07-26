package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.pousheng.middle.open.yunding.JdYunDingSyncStockLogic;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleOrderInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.*;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private MiddleOrderWriteService middleOrderWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @RpcConsumer(check = "false")
    private OpenShopReadService openShopReadService;


    @RpcConsumer(check = "false")
    private ExpressCodeReadService expressCodeReadService;

    @Autowired
    private JdYunDingSyncStockLogic jdYunDingSyncStockLogic;


    public boolean updateOrder(OrderBase orderBase, OrderLevel orderLevel, MiddleOrderEvent orderEvent) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic updateOrder,orderBase {},orderLevel {},orderEvent {}",orderBase,orderLevel,orderEvent);
        }
        Flow flow = flowPicker.pickOrder();

        if (!flow.operationAllowed(orderBase.getStatus(), orderEvent.toOrderOperation())) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", orderBase.getId(), orderBase.getStatus(), orderEvent.toOrderOperation().getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic updateOrder,orderBase {},orderLevel {},orderEvent {},targetStatus {}",orderBase,orderLevel,orderEvent,targetStatus);
        }
        switch (orderLevel) {
            case SHOP:
                Response<Boolean> updateShopOrderResp = orderWriteService.shopOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateShopOrderResp.isSuccess()) {
                    log.error("fail to update shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus, updateShopOrderResp.getError());
                    throw new JsonResponseException(updateShopOrderResp.getError());
                }
                if (log.isDebugEnabled()){
                    log.debug("OrderWriteLogic updateOrder,orderBase {},orderLevel {},orderEvent {},targetStatus {},result {}",orderBase,orderLevel,orderEvent,targetStatus,updateShopOrderResp);
                }
                return updateShopOrderResp.getResult();
            case SKU:
                Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateSkuOrderResp.isSuccess()) {
                    log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus);
                    throw new JsonResponseException(updateSkuOrderResp.getError());
                }
                if (log.isDebugEnabled()){
                    log.debug("OrderWriteLogic updateOrder,orderBase {},orderLevel {},orderEvent {},targetStatus {},result {}",orderBase,orderLevel,orderEvent,targetStatus,updateSkuOrderResp);
                }
                return updateSkuOrderResp.getResult();
            default:
                throw new IllegalArgumentException("unknown.order.type");
        }
    }

    /**
     * 更新子单已处理数量
     *
     * @param skuOrderIdAndQuantity 子单id及数量
     */
    public void updateSkuHandleNumber(Map<Long, Integer> skuOrderIdAndQuantity) {

        log.info("update sku handle number start ....map is {}",skuOrderIdAndQuantity);

        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Flow flow = flowPicker.pickOrder();

        for (SkuOrder skuOrder : skuOrders) {
            //1. 更新extra中剩余待处理数量
            Response<Integer> handleRes = updateSkuOrderExtra(skuOrder, skuOrderIdAndQuantity);
            //2. 判断是否需要更新子单状态
            if (handleRes.isSuccess()) {
                Integer targetStatus;
                //如果剩余数量为0则更新子单状态为待发货
                if (handleRes.getResult() == 0) {
                    targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.HANDLE_DONE.toOrderOperation());
                } else {
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

    private Response<Integer> updateSkuOrderExtra(SkuOrder skuOrder, Map<Long, Integer> skuOrderIdAndQuantity) {
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
        if (waitHandleNumber <= 0) {
            log.error("sku order(id:{}) extra wait handle number:{} ,not enough to ship", skuOrder.getId(), waitHandleNumber);
            return Response.fail("sku.order.wait.handle.number.invalid");
        }
        Integer quantity = skuOrderIdAndQuantity.get(skuOrder.getId());
        Integer remainNumber = waitHandleNumber - quantity;
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
     * 更新shopOrder表的ecpOrderStatus
     *
     * @param shopOrder      店铺订单记录
     * @param orderOperation 操作事件
     */
    public Response<Boolean> updateEcpOrderStatus(ShopOrder shopOrder, OrderOperation orderOperation) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic updateEcpOrderStatus,shopOrder {},orderOperation {}",shopOrder,orderOperation);
        }
        Flow flow = flowPicker.pickEcpOrder();
        String currentStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        if (!flow.operationAllowed(Integer.valueOf(currentStatus), orderOperation)) {
            log.error("shopOrder(id:{}) current status:{} not allow operation:{}", shopOrder.getId(), currentStatus, orderOperation.getText());
            return Response.fail("shopOrder.ecp.status.not.allow.current.operation");
        }
        Integer targetStatus = flow.target(Integer.valueOf(currentStatus), orderOperation);

        Map<String, String> extraMap = shopOrder.getExtra();
        extraMap.put(TradeConstants.ECP_ORDER_STATUS, String.valueOf(targetStatus));
        Response<Boolean> response = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap);
        if (!response.isSuccess()) {
            log.error("update shopOrder：{} extra map to:{} fail,error:{}", shopOrder.getId(), extraMap, response.getError());
            return Response.fail(response.getError());
        }
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic updateEcpOrderStatus,shopOrder {},orderOperation {},result {}",shopOrder,orderOperation,response);
        }
        return Response.ok(Boolean.TRUE);
    }


    /**
     * 取消店铺订单,该店铺订单,sku订单状态为已取消(拉取电商取消整单的订单调用)
     * 电商同步专用
     *
     * @param shopOrderId 店铺订单
     */
    public void autoCancelShopOrder(Long shopOrderId) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic autoCancelShopOrder,shopOrderId {}",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            log.warn("this shopOrder has been canceled,shopOrderId is {}", shopOrderId);
            return;
        }
        //判断该订单是否有取消订单的权限
        if (!validateAutoCancelShopOrder(shopOrder)) {
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}", shopOrder.getId());
            if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YJ.getValue())) {
                throw new OPServerException(200, "shop.order.cancel.failed");
            }
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(), MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("find  shipment by order id:{} level:{} fail,error:{}", shopOrderId, OrderLevel.SHOP.toString(), shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        //取消发货单
        int count = 0;//判断是否存在取消失败的发货单
        for (Shipment shipment : shipments) {
            Response<Boolean> cancelShipmentResponse = shipmentWiteLogic.cancelShipment(shipment);
            if (!cancelShipmentResponse.isSuccess()) {
                //取消失败,后续将整单子单状态设置为取消失败,可以重新发起取消发货单
                count++;
            }
        }
        if (count > 0) {
            //发货单取消失败,订单状态设置为取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation());
            if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.YJ.getValue())) {
                throw new OPServerException(200, "sync.order.to.yyedi.fail");
            }
        } else {
            //发货单取消成功,订单状态设置为取消成功
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
        }

    }

    /**
     * 取消整单失败时,用于补偿失败的订单
     *
     * @param shopOrderId 店铺订单主键
     */
    public Response<Boolean> cancelShopOrder(Long shopOrderId) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic cancelShopOrder,shopOrderId {}",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单是否有取消订单的权限
        if (!validateCancelShopOrder(shopOrder)) {
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}", shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId, MiddleOrderStatus.CANCEL_FAILED.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("find  shipment by order id:{} level:{} fail,error:{}", shopOrderId, OrderLevel.SHOP.toString(), shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        //取消发货单
        Response<Boolean> cancelShipmentResponse = Response.ok(Boolean.TRUE);
        int count = 0;//判断是否存在取消失败的发货单
        String errorMsg = "";
        for (Shipment shipment : shipments) {
            cancelShipmentResponse = shipmentWiteLogic.cancelShipment(shipment);
            if (!cancelShipmentResponse.isSuccess()) {
                errorMsg = cancelShipmentResponse.getError();
                //取消失败,后续将整单子单状态设置为取消失败,可以重新发起取消发货单
                count++;
            }
        }
        if (count > 0) {
            //发货单取消成功,订单状态设置为取消成功
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.CANCEL.toOrderOperation());

        } else {
            //发货单取消失败,订单状态设置为取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
        }
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic cancelShopOrder,shopOrderId {},count {}",shopOrderId,count);
        }
        if (count > 0) {
            return Response.fail(errorMsg);
        } else {
            return Response.ok(Boolean.TRUE);
        }
    }

    /**
     * 取消订单中的子订单(拉取电商取消子单的订单调用)
     * 电商同步专用
     *
     * @param shopOrderId 店铺订单主键
     * @param skuCode     子单代码
     */
    public void autoCancelSkuOrder(long shopOrderId, String skuCode) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic autoCancelSkuOrder,shopOrderId {},skuCode {}",shopOrderId,skuCode);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            log.warn("this shopOrder has been canceled,shopOrderId is {}", shopOrderId);
            return;
        }
        //判断该订单所属整单是否有取消订单的权限
        if (!validateAutoCancelShopOrder4Sku(shopOrder)) {
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}", shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(), MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue());
        SkuOrder skuOrder = this.getSkuOrder(skuOrders, skuCode);
        //判断子单是否存在以及子单状态是否已经发生变化
        if (skuOrder == null || Objects.equals(skuOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            log.warn("skuOrder status changed,shopOrderId is {},skuCode is {}", shopOrder, skuCode);
            return;
        }
        //判断订单是否有取消订单的权限
        if (!validateAutoCancelSkuOrder(skuOrder)) {
            log.error("this skuOrder can not be canceled,because of error skuOrder status.shopOrderId is :{}", skuOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //取消所有的发货单
        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("find  shipment by order id:{} level:{} fail,error:{}", shopOrderId, OrderLevel.SHOP.toString(), shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        //其他需要恢复成待处理状态的子单
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).filter(it -> !Objects.equals(it.getId(), skuOrder.getId())).collect(Collectors.toList());

        int count = 0;//计数器用来记录是否有发货单取消失败
        for (Shipment shipment : shipments) {
            Response<Boolean> cancelShipmentResponse = shipmentWiteLogic.cancelShipment(shipment);
            if (!cancelShipmentResponse.isSuccess()) {
                count++;
            }
        }
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic autoCancelSkuOrder,shopOrderId {},skuCode {},count {}",shopOrderId,skuCode,count);
        }
        if (count > 0) {
            //子单取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder, Lists.newArrayList(), skuOrder,
                    MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation(), MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation(), skuCode);
        } else {
            //子单取消成功
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder, skuOrdersFilter, skuOrder,
                    MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation(), MiddleOrderEvent.REVOKE.toOrderOperation(), "");
            if (response.isSuccess()) {
                //子单撤销成功之后如果存在其他的子单,则需要自动生成发货单
                log.info("after auto cancel sku order,try to auto create shipment,shopOrder id is {}", shopOrder.getId());
                shipmentWiteLogic.doAutoCreateShipment(shopOrder);
            }

        }
    }

    /**
     * 取消订单中的子订单(子单取消失败时调用)
     * 电商同步专用
     *
     * @param shopOrderId 店铺订单主键
     * @param skuCode     子单代码
     */
    public Response<Boolean> cancelSkuOrder(long shopOrderId, String skuCode) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic cancelSkuOrder,shopOrderId {},skuCode {}",shopOrderId,skuCode);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //如果订单是已经取消,直接返回
        if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            return Response.ok(Boolean.TRUE);
        }
        //判断该订单所属整单是否有取消订单的权限
        if (!validateCancelShopOrder4Sku(shopOrder)) {
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}", shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId, MiddleOrderStatus.CANCEL_FAILED.getValue(), MiddleOrderStatus.WAIT_HANDLE.getValue());
        SkuOrder skuOrder = this.getSkuOrder(skuOrders, skuCode);
        //判断子单是否存在以及子单状态是否已经发生变化
        if (skuOrder == null || Objects.equals(skuOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            log.warn("skuOrder status changed,shopOrderId is {},skuCode is {}", shopOrder, skuCode);
            throw new JsonResponseException("skuOrder.status.changed");
        }
        //判断该订单是否有取消订单的权限
        if (!validateCancelSkuOrder(skuOrder)) {
            log.error("this skuOrder can not be canceled,because of error skuOrder status.shopOrderId is :{}", skuOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //取消所有的发货单
        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("find  shipment by order id:{} level:{} fail,error:{}", shopOrderId, OrderLevel.SHOP.toString(), shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        //获取需要恢复成待处理状态的子单
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).filter(it -> !Objects.equals(it.getId(), skuOrder.getId())).collect(Collectors.toList());
        int count = 0;//计数器用来记录是否有发货单取消失败
        String errorMsg = "";
        for (Shipment shipment : shipments) {
            Response<Boolean> cancelShipmentResponse = shipmentWiteLogic.cancelShipment(shipment);
            if (!cancelShipmentResponse.isSuccess()) {
                errorMsg = cancelShipmentResponse.getError();
                count++;
            }
        }
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic cancelSkuOrder,shopOrderId {},skuCode {},count {}",shopOrderId,skuCode,count);
        }
        if (count > 0) {
            middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder, Lists.newArrayList(),
                    skuOrder, MiddleOrderEvent.CANCEL.toOrderOperation(), MiddleOrderEvent.CANCEL.toOrderOperation(), skuCode);

        } else {
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder, skuOrdersFilter,
                    skuOrder, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation(), MiddleOrderEvent.REVOKE_SUCCESS.toOrderOperation(), "");
            if (response.isSuccess()) {
                //子单撤销成功之后如果存在其他的子单,则需要自动生成发货单
                log.info("after cancel sku order,try to auto create shipment,shopOrder id is {}", shopOrder.getId());
                shipmentWiteLogic.doAutoCreateShipment(shopOrder);
            }

        }
        if (count > 0) {
            return Response.fail(errorMsg);
        } else {
            return Response.ok(Boolean.TRUE);
        }
    }

    /**
     * 撤销店铺订单,该店铺订单,sku订单状态恢复到初始状态,可用于手工调用
     *
     * @param shopOrderId 店铺订单主键
     */
    public Response<Boolean> rollbackShopOrder(Long shopOrderId) {
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic rollbackShopOrder,shopOrderId {}",shopOrderId);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单是否有撤销订单的权限
        if (!validateRollbackShopOrder(shopOrder)) {
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}", shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(), MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue(), MiddleOrderStatus.REVOKE_FAILED.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId, OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()) {
            log.error("find  shipment by order id:{} level:{} fail,error:{}", shopOrderId, OrderLevel.SHOP.toString(), shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it -> !Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());

        //取消发货单
        int count = 0;//计数器用来记录是否有发货单取消失败的
        String errorMsg = "";
        List<SkuOrder> failSkuOrders = new ArrayList<SkuOrder>();
        for (Shipment shipment : shipments) {
            Response<Boolean> cancelShipmentResponse = shipmentWiteLogic.cancelShipment(shipment);
            if (!cancelShipmentResponse.isSuccess()) {
                errorMsg = cancelShipmentResponse.getError();
                count++;
                //如果取消发货单失败，则只更新相应的子单状态
                failSkuOrders.addAll(skuOrders.stream().filter(skuOrder -> shipment.getSkuInfos().containsKey(skuOrder.getId())).collect(Collectors.toList()));
            }
        }
        if (log.isDebugEnabled()){
            log.debug("OrderWriteLogic rollbackShopOrder,shopOrderId {},count {}",shopOrderId,count);
        }

        if (count > 0) {
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, failSkuOrders, MiddleOrderEvent.REVOKE_FAIL.toOrderOperation());
            if (!response.isSuccess()){
                log.error("call updateOrderStatusAndSkuQuantities fail,shop order:{},sku order:{} operation:{} fail,error:{}",shopOrder,failSkuOrders,MiddleOrderEvent.REVOKE_FAIL.toOrderOperation());
                return Response.fail(response.getError());
            }
            //将处理失败的从skuOrders中移除 认为是处理成功的
            skuOrders.removeAll(failSkuOrders);
        }
        if (skuOrders.size() > 0) {
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.REVOKE.toOrderOperation());
            if (!response.isSuccess()) {
                log.error("call updateOrderStatusAndSkuQuantities fail,shop order:{},sku order:{} operation:{} fail,error:{}", shopOrder, skuOrders, MiddleOrderEvent.REVOKE.toOrderOperation());
                return Response.fail(response.getError());

            }
        }
        if (count > 0) {
            return Response.fail(errorMsg);
        } else {
            return Response.ok(Boolean.TRUE);
        }
    }

    /**
     * 判断整单是否有取消的权限(自动取消整单)
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateAutoCancelShopOrder(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断整单是否有取消的权限(自动取消失败时,手动取消整单)
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateCancelShopOrder(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.CANCEL.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断整单是否有取消的权限(给自动取消子单使用)
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateAutoCancelShopOrder4Sku(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断整单是否有取消的权限(给手动取消子单使用)
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateCancelShopOrder4Sku(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.CANCEL.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断整单是否有撤销的权限
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateRollbackShopOrder(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.REVOKE.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断子单是否可以取消(自动取消)
     *
     * @param skuOrder 店铺订单
     * @return 可以取消(true), 不可取消(false)
     */
    private boolean validateAutoCancelSkuOrder(SkuOrder skuOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = skuOrder.getStatus();
        return orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
    }

    /**
     * 判断子单是否可以取消(取消子单失败,手动取消)
     *
     * @param skuOrder 子单
     * @return 判断子单的状态是否有下一步的操作, 如果有返回true
     */
    private boolean validateCancelSkuOrder(SkuOrder skuOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = skuOrder.getStatus();
        return orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.CANCEL.toOrderOperation());
    }

    /**
     * 根据skuCode获取skuOrder
     *
     * @param skuOrders 子单集合
     * @param skuCode   sku代码
     * @return 返回经过过滤的skuOrder记录
     */
    private SkuOrder getSkuOrder(List<SkuOrder> skuOrders, String skuCode) {
        Optional<SkuOrder> skuOrderOptional = skuOrders.stream().filter(Objects::nonNull).filter(it -> Objects.equals(it.getSkuCode(), skuCode)).findAny();
        if (skuOrderOptional.isPresent()) {
            return skuOrderOptional.get();
        } else {
            return null;
        }
    }

    /**
     * 添加店铺订单客服备注
     *
     * @param shopOrderId         店铺订单主键
     * @param customerServiceNote 客服备注
     */
    public void addCustomerServiceNote(long shopOrderId, String customerServiceNote) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Map<String, String> map = shopOrder.getExtra();
        map.put(TradeConstants.CUSTOMER_SERVICE_NOTE, customerServiceNote);
        Response<Boolean> response = orderWriteService.updateOrderExtra(shopOrderId, OrderLevel.SHOP, map);
        if (!response.isSuccess()) {
            log.error("shopOrder add customerServiceNote failed,shopOrderId is({}),caused by{}", shopOrderId, response.getError());
            throw new JsonResponseException("add customer service note fail");
        }
    }

    /**
     * 将文件中的订单按照外部订单号以及来源渠道分组
     *
     * @param orderInfos
     * @return
     */
    public List<OpenFullOrderInfo> groupByMiddleOrderInfo(List<MiddleOrderInfo> orderInfos) {
        //按照渠道来源分组
        ListMultimap<String, MiddleOrderInfo> channelMulitMaps = Multimaps.index(orderInfos, new Function<MiddleOrderInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable MiddleOrderInfo orderInfo) {
                return orderInfo.getChannel();
            }
        });
        //遍历按照channel分组的参数
        List<OpenFullOrderInfo> openFullOrderInfos = Lists.newArrayList();
        for (String channel : channelMulitMaps.keySet()) {
            //获取每个channel的订单集合
            List<MiddleOrderInfo> middleOrderInfos = channelMulitMaps.get(channel);
            //订单按照订单号进行分组
            ListMultimap<String, MiddleOrderInfo> outOrderIdMulitMaps = Multimaps.index(middleOrderInfos, new Function<MiddleOrderInfo, String>() {
                @Nullable
                @Override
                public String apply(@Nullable MiddleOrderInfo orderInfo) {
                    return orderInfo.getOutOrderId();
                }
            });
            for (String outOrderId : outOrderIdMulitMaps.keySet()) {
                //获取每个channel，每个outOrderId下面的订单集合列表
                List<MiddleOrderInfo> orderInfoList = outOrderIdMulitMaps.get(outOrderId);
                if (!orderInfoList.isEmpty()) {
                    OpenFullOrderInfo openFullOrderInfo = this.makeOpenFullOrderInfo(orderInfoList);
                    openFullOrderInfos.add(openFullOrderInfo);
                }
            }
        }
        return openFullOrderInfos;
    }

    /**
     * 组装订单参数
     *
     * @param orderInfoList
     * @return
     */
    private static final Map<String, String> CHANNEL = Maps.newHashMap();

    static {
        //外部店铺渠道名称映射
        CHANNEL.put("官网", "official");
        CHANNEL.put("天猫", "taobao");
        CHANNEL.put("京东", "jingdong");
        CHANNEL.put("苏宁", "suning");
        CHANNEL.put("分期乐", "fenqile");
        CHANNEL.put("淘宝", "taobao");
    }

    private static final Map<String, Integer> PAY_TYPE = Maps.newHashMap();

    static {
        //支付方式
        PAY_TYPE.put("在线支付", 1);
        PAY_TYPE.put("货到付款", 2);
    }

    private static final Set<String> PAY_CHANNEL = Sets.newHashSet("微信支付", "京东支付", "网银支付", "支付宝");


    private OpenFullOrderInfo makeOpenFullOrderInfo(List<MiddleOrderInfo> orderInfoList) {
        OpenFullOrderInfo openFullOrderInfo = new OpenFullOrderInfo();
        OpenFullOrder order = new OpenFullOrder();
        List<OpenFullOrderItem> openFullOrderItems = Lists.newArrayList();
        OpenFullOrderInvoice invoice = new OpenFullOrderInvoice();
        OpenFullOrderAddress address = new OpenFullOrderAddress();

        BeanUtils.copyProperties(orderInfoList.get(0), order);
        BeanUtils.copyProperties(orderInfoList.get(0), address);

        //参数校验
        checkNotNull(orderInfoList.get(0).getOutOrderId(), "out.order.id.not.null");
        checkNotNull(orderInfoList.get(0).getChannel(), "order.channel.not.null");
        checkNotNull(orderInfoList.get(0).getBuyerName(), "order.buyer.name.not.null");
        checkNotNull(orderInfoList.get(0).getBuyerMobile(), "order.buyer.mobile.not.null");
        checkNotNull(orderInfoList.get(0).getReceiveUserName(), "order.receiveUserName.not.null");
        checkNotNull(orderInfoList.get(0).getMobile(), "order.mobile.not.null");
        checkNotNull(orderInfoList.get(0).getProvince(), "order.province.id.not.null");
        checkNotNull(orderInfoList.get(0).getCity(), "order.city.not.null");
        checkNotNull(orderInfoList.get(0).getRegion(), "order.region.not.null");
        checkNotNull(orderInfoList.get(0).getDetail(), "order.detail.not.null");
        checkNotNull(orderInfoList.get(0).getShopName(), "order.shopName.not.null");
        checkNotNull(orderInfoList.get(0).getOrderHkExpressName(), "order.express.name.not.null");
        checkNotNull(orderInfoList.get(0).getShipFee(), "order.shipFee.not.null");
        checkNotNull(orderInfoList.get(0).getFee(), "order.fee.not.null");
        checkNotNull(orderInfoList.get(0).getOrderOriginFee(), "order.orderOriginFee.not.null");
        checkNotNull(orderInfoList.get(0).getDiscount(), "order.discount.not.null");
        checkNotNull(orderInfoList.get(0).getCreatedAt(), "order.createdAt.not.null");

        //检测订单来源渠道
        if (Arguments.isNull(CHANNEL.get(orderInfoList.get(0).getChannel()))) {
            throw new ServiceException("外部订单号 " + orderInfoList.get(0).getOutOrderId() + " 订单来源渠道不合法");
        }
        //渠道替换成相应代码
        order.setChannel(CHANNEL.get(orderInfoList.get(0).getChannel()));

        //校验外部店铺名称是否存在
        OpenShop openShop = RespHelper.orServEx(openShopReadService.findByChannelAndName(
                CHANNEL.get(orderInfoList.get(0).getChannel()), orderInfoList.get(0).getShopName()));
        if (Arguments.isNull(openShop)) {
            throw new ServiceException(orderInfoList.get(0).getShopName() + "店铺不存在");
        }
        order.setShopId(openShop.getId());
        order.setShopName(orderInfoList.get(0).getShopName());

        //如果填写物流名称  则校验物流名称是否存在,查找不到先不抛出异常
        if (StringUtils.hasText(orderInfoList.get(0).getOrderHkExpressName())) {
            ExpressCode expressCode = RespHelper.orServEx(expressCodeReadService.findByName(orderInfoList.get(0).getOrderHkExpressName()));
            if (Arguments.notNull(expressCode)) {
                order.setOrderExpressCode(expressCode.getFenqileCode());
                order.setOrderHkExpressName(orderInfoList.get(0).getOrderHkExpressName());
            } else {
                order.setOrderExpressCode(orderInfoList.get(0).getOrderHkExpressName());
            }
        }
        // 发票信息判断,发票选填
        // 抬头类型（titleType：1个人，2公司）
        // 发票类型（invoiceType：1普通发票，2增值税发票，3电子发票）
        String titleType;
        String invoiceType;
        if (Arguments.notNull(orderInfoList.get(0).getTitleType()) || Arguments.notNull(orderInfoList.get(0).getInvoiceType())) {
            if (Objects.equals("个人", orderInfoList.get(0).getTitleType())) {
                titleType = "1";
            } else if (Objects.equals("公司", orderInfoList.get(0).getTitleType())) {
                checkNotNull(orderInfoList.get(0).getTaxRegisterNo(), "order.TaxRegisterNo.not.null");
                checkNotNull(orderInfoList.get(0).getCompanyName(), "order.company.name.not.null");
                titleType = "2";
            } else {
                throw new ServiceException("order.title.type.not.legal");
            }

            if (Objects.equals("普通发票", orderInfoList.get(0).getInvoiceType())) {
                invoiceType = "1";
            } else if (Objects.equals("增值税发票", orderInfoList.get(0).getInvoiceType())) {

                //注册地址 注册电话  开户银行 银行账号   单位名称 纳税人识别号
                checkNotNull(orderInfoList.get(0).getRegisterAddress(), "order.registerAddress.not.null");
                checkNotNull(orderInfoList.get(0).getRegisterPhone(), "order.registerPhone.not.null");
                checkNotNull(orderInfoList.get(0).getRegisterBank(), "order.registerBank.not.null");
                checkNotNull(orderInfoList.get(0).getBankAccount(), "order.bankAccount.not.null");
                invoiceType = "2";
            } else if (Objects.equals("电子发票", orderInfoList.get(0).getInvoiceType())) {
                checkNotNull(orderInfoList.get(0).getRegisterPhone(), "order.registerPhone.not.null");
                checkNotNull(orderInfoList.get(0).getEmail(), "order.email.not.null");
                invoiceType = "3";
                invoice.setMobile(orderInfoList.get(0).getRegisterPhone());
            } else {
                throw new ServiceException("order.invoice.type.not.legal");
            }
            invoice.setInvoiceType(invoiceType);
            invoice.setTitleType(titleType);
            invoice.setTaxRegisterNo(orderInfoList.get(0).getTaxRegisterNo());
            invoice.setCompanyName(orderInfoList.get(0).getCompanyName());
            invoice.setRegisterPhone(orderInfoList.get(0).getRegisterPhone());
            invoice.setRegisterAddress(orderInfoList.get(0).getRegisterAddress());
            invoice.setRegisterBank(orderInfoList.get(0).getRegisterBank());
            invoice.setBankAccount(orderInfoList.get(0).getBankAccount());
            invoice.setEmail(orderInfoList.get(0).getEmail());
        }

        //支付方式选择
        if (!Strings.isNullOrEmpty(orderInfoList.get(0).getPayType())) {
            if (Arguments.isNull(PAY_TYPE.get(orderInfoList.get(0).getPayType()))) {
                throw new ServiceException("order.pay.type.not.legal");
            }
            order.setPayType(PAY_TYPE.get(orderInfoList.get(0).getPayType()));
        }
        //支付渠道选择
        if (!Strings.isNullOrEmpty(orderInfoList.get(0).getPaymentChannelName())) {
            if (!PAY_CHANNEL.contains(orderInfoList.get(0).getPaymentChannelName())) {
                throw new ServiceException("order.pay.channel.not.legal");
            }
        }

        //原始运费设置成实际运费
        order.setOriginShipFee(Long.valueOf(orderInfoList.get(0).getShipFee()));
        order.setShipFee(Long.valueOf(orderInfoList.get(0).getShipFee()));
        order.setFee(Long.valueOf(orderInfoList.get(0).getFee()));
        order.setOriginFee(Long.valueOf(orderInfoList.get(0).getOrderOriginFee()));
        order.setDiscount(Long.valueOf(orderInfoList.get(0).getDiscount()));

        order.setStatus(1);

        //子订单号必填，且同一个订单不能重复
        Set<String> outSkuOrderIds = Sets.newHashSet();
        for (MiddleOrderInfo middleOrderInfo : orderInfoList) {
            checkNotNull(middleOrderInfo.getOutSkuOrderId(), "order.out.sku.id.not.null");
            outSkuOrderIds.add(middleOrderInfo.getOutSkuOrderId());

            OpenFullOrderItem openFullOrderItem = new OpenFullOrderItem();
            openFullOrderItem.setOutSkuorderId(middleOrderInfo.getOutSkuOrderId());
            openFullOrderItem.setOriginFee(Long.valueOf(middleOrderInfo.getOriginFee()));
            openFullOrderItem.setQuantity(Integer.valueOf(middleOrderInfo.getQuantity()));
            openFullOrderItem.setSkuCode(middleOrderInfo.getSkuCode());
            openFullOrderItem.setDiscount(Long.valueOf(middleOrderInfo.getItemDiscount()));
            openFullOrderItem.setItemName(middleOrderInfo.getItemName());
            openFullOrderItem.setChannelItemId(middleOrderInfo.getItemId());
            openFullOrderItems.add(openFullOrderItem);
        }
        if (orderInfoList.size() != outSkuOrderIds.size()) {
            throw new ServiceException("order.out.sku.id.duplicate");
        }
        openFullOrderInfo.setOrder(order);
        openFullOrderInfo.setItem(openFullOrderItems);
        openFullOrderInfo.setAddress(address);
        openFullOrderInfo.setInvoice(invoice);
        return openFullOrderInfo;
    }

    /**
     * 天猫订单修复
     *
     * @param shopId
     */
    public void updateOrderAmount(Long shopId) {
        int pageNo = 1;
        while (true) {
            MiddleOrderCriteria criteria = new MiddleOrderCriteria();
            criteria.setShopId(shopId);
            criteria.setStatus(Lists.newArrayList(1, 2, 3, 4, 5, 6, -1, -2, -3, -4, -5, -6));
            criteria.setPageNo(pageNo);
            log.info("pageNo i=========s {}", pageNo);
            Response<Paging<ShopOrder>> r = middleOrderReadService.pagingShopOrder(criteria);
            if (r.isSuccess()) {
                Paging<ShopOrder> shopOrderPaging = r.getResult();
                List<ShopOrder> shopOrders = shopOrderPaging.getData();
                if (!shopOrders.isEmpty()) {
                    for (ShopOrder shopOrder : shopOrders) {
                        Response<OpenClientFullOrder> orderResponse = orderServiceCenter.findById(shopId, shopOrder.getOutId());
                        if (orderResponse.isSuccess()) {
                            OpenClientFullOrder openClientFullOrder = orderResponse.getResult();
                            ShopOrder newShopOrder = new ShopOrder();
                            newShopOrder.setId(shopOrder.getId());
                            newShopOrder.setFee(openClientFullOrder.getFee());
                            newShopOrder.setDiscount(openClientFullOrder.getDiscount());
                            newShopOrder.setShipFee(openClientFullOrder.getShipFee());
                            newShopOrder.setOriginShipFee(openClientFullOrder.getShipFee());
                            Response<Boolean> shopOrderR = middleOrderWriteService.updateShopOrder(newShopOrder);
                            if (!shopOrderR.isSuccess()) {
                                log.error("shopOrder failed,id is {}", shopOrder.getId());
                            } else {
                                List<OpenClientOrderItem> items = openClientFullOrder.getItems();
                                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
                                for (SkuOrder skuOrder : skuOrders) {
                                    for (OpenClientOrderItem item : items) {
                                        if (Objects.equals(skuOrder.getOutSkuId(), item.getSkuId())) {
                                            log.info("update skuOrder");
                                            SkuOrder newSkuOrder = new SkuOrder();
                                            newSkuOrder.setId(skuOrder.getId());
                                            newSkuOrder.setOriginFee(Long.valueOf(item.getPrice() * item.getQuantity()));
                                            newSkuOrder.setDiscount(Long.valueOf(item.getDiscount()));
                                            newSkuOrder.setFee(newSkuOrder.getOriginFee() - newSkuOrder.getDiscount());
                                            Response<Boolean> skuOrderR = middleOrderWriteService.updateSkuOrder(newSkuOrder);
                                            if (!skuOrderR.isSuccess()) {
                                                log.error("skuOrder failed,id is", newSkuOrder.getId());
                                            }
                                        } else {
                                            log.info("do not update skuOrder");
                                        }
                                    }
                                }
                            }
                        } else {
                            log.info("shop order failed");
                        }
                    }
                } else {
                    break;
                }
                pageNo++;
            }
        }
    }

    /**
     * 天猫订单修复
     *
     * @param shopId
     */
    public void updateOrderAmountByOrderId(Long shopId, Long shopOrderId) {
        log.info("order amount recover ,shopOrderId={},shopId={}",shopOrderId,shopId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Response<OpenClientFullOrder> orderResponse = orderServiceCenter.findById(shopId, shopOrder.getOutId());
        if (orderResponse.isSuccess()) {
            OpenClientFullOrder openClientFullOrder = orderResponse.getResult();
            log.info("find open client full order,openClientFullOrder={}",openClientFullOrder);
            ShopOrder newShopOrder = new ShopOrder();
            newShopOrder.setId(shopOrder.getId());
            newShopOrder.setFee(openClientFullOrder.getFee());
            newShopOrder.setDiscount(openClientFullOrder.getDiscount());
            newShopOrder.setShipFee(openClientFullOrder.getShipFee());
            newShopOrder.setOriginShipFee(openClientFullOrder.getShipFee());
            Response<Boolean> shopOrderR = middleOrderWriteService.updateShopOrder(newShopOrder);
            if (!shopOrderR.isSuccess()) {
                log.error("shopOrder failed,id is {}", shopOrder.getId());
            } else {
                List<OpenClientOrderItem> items = openClientFullOrder.getItems();
                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
                for (SkuOrder skuOrder : skuOrders) {
                    for (OpenClientOrderItem item : items) {
                        if (Objects.equals(skuOrder.getOutSkuId(), item.getSkuId())) {
                            log.info("update skuOrder");
                            SkuOrder newSkuOrder = new SkuOrder();
                            newSkuOrder.setId(skuOrder.getId());
                            newSkuOrder.setOriginFee(Long.valueOf(item.getPrice() * item.getQuantity()));
                            newSkuOrder.setDiscount(Long.valueOf(item.getDiscount()));
                            newSkuOrder.setFee(newSkuOrder.getOriginFee() - newSkuOrder.getDiscount());
                            Response<Boolean> skuOrderR = middleOrderWriteService.updateSkuOrder(newSkuOrder);
                            if (!skuOrderR.isSuccess()) {
                                log.error("skuOrder failed,id is", newSkuOrder.getId());
                            }
                        } else {
                            log.info("do not update skuOrder");
                        }
                    }
                }
            }
        } else {
            log.info("shop order failed");
        }
    }



    /**
     * 天猫订单修复
     *
     * @param shopId
     */
    public void updateJdYunDingOrderAmount(Long shopId) {
        int pageNo = 1;
        while (true) {
            MiddleOrderCriteria criteria = new MiddleOrderCriteria();
            criteria.setShopId(shopId);
            criteria.setStatus(Lists.newArrayList(1, 2, 3, 4, 5, 6, -1, -2, -3, -4, -5, -6));
            criteria.setPageNo(pageNo);
            log.info("pageNo i=========s {}", pageNo);
            Response<Paging<ShopOrder>> r = middleOrderReadService.pagingShopOrder(criteria);
            if (r.isSuccess()) {
                Paging<ShopOrder> shopOrderPaging = r.getResult();
                List<ShopOrder> shopOrders = shopOrderPaging.getData();
                if (!shopOrders.isEmpty()) {
                    for (ShopOrder shopOrder : shopOrders) {
                        jdYunDingSyncStockLogic.syncUpdateJdOrderAmount(shopOrder.getShopId(),shopOrder.getOutId());
                    }
                } else {
                    break;
                }
                pageNo++;
            }
        }
    }

    /**
     * 天猫订单修复
     *
     * @param shopId
     */
    public void updateJdYundingOrderAmountByOrderId(Long shopId, Long shopOrderId) {
        log.info("order amount recover ,shopOrderId={},shopId={}",shopOrderId,shopId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        jdYunDingSyncStockLogic.syncUpdateJdOrderAmount(shopOrder.getShopId(),shopOrder.getOutId());
    }
}

