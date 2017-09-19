package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private MiddleOrderWriteService middleOrderWriteService;
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

        if (!flow.operationAllowed(orderBase.getStatus(), orderEvent.toOrderOperation())) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", orderBase.getId(), orderBase.getStatus(), orderEvent.toOrderOperation().getText());
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
     *
     * @param skuOrderIdAndQuantity 子单id及数量
     */
    public void updateSkuHandleNumber(Map<Long, Integer> skuOrderIdAndQuantity) {

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
        return Response.ok();
    }


    /**
     * 取消店铺订单,该店铺订单,sku订单状态为已取消(拉取电商取消整单的订单调用)
     * 电商同步专用
     * @param shopOrderId 店铺订单
     */
    public void autoCancelShopOrder(Long shopOrderId) {

        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单是否有取消订单的权限
        if (!validateAutoCancelShopOrder(shopOrder)){
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}",shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()){
            log.error("find  shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
        //取消发货单
        int count=0;//判断是否存在取消失败的发货单
        for (Shipment shipment:shipments){
            if (!shipmentWiteLogic.cancelShipment(shipment,0))
            {
                //取消失败,后续将整单子单状态设置为取消失败,可以重新发起取消发货单
                count++;
            }
        }
        if (count>0)
        {
            //发货单取消失败,订单状态设置为取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation());
        }else {
            //发货单取消成功,订单状态设置为取消成功
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
        }

    }
    /**
     *取消整单失败时,用于补偿失败的订单
     * @param shopOrderId 店铺订单主键
     */
    public void cancelShopOrder(Long shopOrderId) {

        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单是否有取消订单的权限
        if (!validateCancelShopOrder(shopOrder)){
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}",shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId, MiddleOrderStatus.CANCEL_FAILED.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()){
            log.error("find  shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
        //取消发货单
        int count=0;//判断是否存在取消失败的发货单
        for (Shipment shipment:shipments){
            if (!shipmentWiteLogic.cancelShipment(shipment,0))
            {
                //取消失败,后续将整单子单状态设置为取消失败,可以重新发起取消发货单
                count++;
            }
        }
        if (count>0)
        {
            //发货单取消成功,订单状态设置为取消成功
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.CANCEL.toOrderOperation());

        }else {
            //发货单取消失败,订单状态设置为取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
        }
    }
    /**
     * 取消订单中的子订单(拉取电商取消子单的订单调用)
     * 电商同步专用
     * @param shopOrderId 店铺订单主键
     * @param skuCode     子单代码
     */
    public void autoCancelSkuOrder(long shopOrderId, String skuCode) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单所属整单是否有取消订单的权限
        if (!validateAutoCancelShopOrder4Sku(shopOrder)){
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}",shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue());
        SkuOrder skuOrder = this.getSkuOrder(skuOrders,skuCode);
        //判断订单是否有取消订单的权限
        if (!validateAutoCancelSkuOrder(skuOrder)){
            log.error("this skuOrder can not be canceled,because of error skuOrder status.shopOrderId is :{}",skuOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //取消所有的发货单
        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()){
            log.error("find  shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
        //其他需要恢复成待处理状态的子单
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).filter(it->!Objects.equals(it.getId(),skuOrder.getId())).collect(Collectors.toList());

        int count=0;//计数器用来记录是否有发货单取消失败
        for (Shipment shipment:shipments){
            if (!shipmentWiteLogic.cancelShipment(shipment,0))
            {
                count++;
            }
        }
        if (count>0){
            //子单取消失败
            middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrdersFilter,skuOrder,
                    MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation(),MiddleOrderEvent.AUTO_CANCEL_FAIL.toOrderOperation(),skuCode);
        }else{
            //子单取消成功
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrdersFilter,skuOrder,
                    MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation(),MiddleOrderEvent.REVOKE.toOrderOperation(),"");
            if (response.isSuccess()){
                //子单撤销成功之后如果存在其他的子单,则需要自动生成发货单
                shipmentWiteLogic.doAutoCreateShipment(shopOrder);
            }

        }
    }

    /**
     * 取消订单中的子订单(子单取消失败时调用)
     * 电商同步专用
     * @param shopOrderId 店铺订单主键
     * @param skuCode     子单代码
     */
    public void cancelSkuOrder(long shopOrderId, String skuCode) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单所属整单是否有取消订单的权限
        if (!validateCancelShopOrder4Sku(shopOrder)){
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}",shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,MiddleOrderStatus.CANCEL_FAILED.getValue(),MiddleOrderStatus.WAIT_HANDLE.getValue());
        SkuOrder skuOrder = this.getSkuOrder(skuOrders,skuCode);
        //判断该订单是否有取消订单的权限
        if (!validateCancelSkuOrder(skuOrder)){
            log.error("this skuOrder can not be canceled,because of error skuOrder status.shopOrderId is :{}",skuOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //取消所有的发货单
        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()){
            log.error("find  shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());
        //获取需要恢复成待处理状态的子单
        List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(Objects::nonNull).filter(it->!Objects.equals(it.getId(),skuOrder.getId())).collect(Collectors.toList());
        int count=0;//计数器用来记录是否有发货单取消失败
        for (Shipment shipment:shipments){
            if (!shipmentWiteLogic.cancelShipment(shipment,0))
            {
                count++;
            }
        }
        if (count>0){
            middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrdersFilter,
                    skuOrder,MiddleOrderEvent.CANCEL.toOrderOperation(),MiddleOrderEvent.CANCEL.toOrderOperation(),skuCode);

        }else{
            Response<Boolean> response = middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrdersFilter,
                   skuOrder,MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation(),MiddleOrderEvent.REVOKE_SUCCESS.toOrderOperation(),"");
            if (response.isSuccess()){
                //子单撤销成功之后如果存在其他的子单,则需要自动生成发货单
                shipmentWiteLogic.doAutoCreateShipment(shopOrder);
            }

        }
    }

    /**
     * 撤销店铺订单,该店铺订单,sku订单状态恢复到初始状态,可用于手工调用
     *
     * @param shopOrderId 店铺订单主键
     */
    public void rollbackShopOrder(Long shopOrderId) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //判断该订单是否有撤销订单的权限
        if (!validateRollbackShopOrder(shopOrder)){
            log.error("this shopOrder can not be canceled,because of error shopOrder status.shopOrderId is :{}",shopOrder.getId());
            throw new JsonResponseException("shop.order.cancel.failed");
        }
        //获取该订单下所有的子单和发货单
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrderId,
                MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                MiddleOrderStatus.WAIT_SHIP.getValue(),MiddleOrderStatus.REVOKE_FAILED.getValue());

        Response<List<Shipment>> shipmentsRes = shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!shipmentsRes.isSuccess()){
            log.error("find  shipment by order id:{} level:{} fail,error:{}",shopOrderId,OrderLevel.SHOP.toString(),shipmentsRes.getError());
            throw new JsonResponseException("find.shipment.failed");
        }
        List<Shipment> shipments = shipmentsRes.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).collect(Collectors.toList());

        //取消发货单
        int count= 0 ;//计数器用来记录是否有发货单取消失败的
        for (Shipment shipment:shipments){
            if (!shipmentWiteLogic.cancelShipment(shipment,1))
            {
                count++;
            }
        }

        if (count>0){
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.REVOKE_FAIL.toOrderOperation());
        }else{
            middleOrderWriteService.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,MiddleOrderEvent.REVOKE.toOrderOperation());
        }



    }

    /**
     * 判断整单是否有取消的权限(自动取消整单)
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true),不可取消(false)
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
     * @return 可以取消(true),不可取消(false)
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
     * @return 可以取消(true),不可取消(false)
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
     * @return 可以取消(true),不可取消(false)
     */
    private boolean validateCancelShopOrder4Sku(ShopOrder shopOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = shopOrder.getStatus();
        String ecpStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        return (orderFlow.operationAllowed(sourceStatus,MiddleOrderEvent.CANCEL.toOrderOperation()))
                && (Objects.equals(Integer.valueOf(ecpStatus), EcpOrderStatus.WAIT_SHIP.getValue()));
    }

    /**
     * 判断整单是否有撤销的权限
     *
     * @param shopOrder 店铺订单
     * @return 可以取消(true),不可取消(false)
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
     * @return  可以取消(true),不可取消(false)
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
     * @return  判断子单的状态是否有下一步的操作,如果有返回true
     */
    private boolean validateCancelSkuOrder(SkuOrder skuOrder) {
        Flow orderFlow = flowPicker.pickOrder();
        Integer sourceStatus = skuOrder.getStatus();
        return orderFlow.operationAllowed(sourceStatus, MiddleOrderEvent.CANCEL.toOrderOperation());
    }
    /**
     * 根据skuCode获取skuOrder
     * @param skuOrders 子单集合
     * @param skuCode sku代码
     * @return 返回经过过滤的skuOrder记录
     */
    private SkuOrder getSkuOrder(List<SkuOrder> skuOrders, String skuCode){
        return skuOrders.stream().filter(Objects::nonNull).filter(it->Objects.equals(it.getSkuCode(),skuCode)).collect(Collectors.toList()).get(0);
    }

    /**
     * 添加店铺订单客服备注
     * @param shopOrderId 店铺订单主键
     * @param customerServiceNote 客服备注
     */
    public void addCustomerServiceNote(long shopOrderId,String  customerServiceNote){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Map<String,String> map = shopOrder.getExtra();
        map.put(TradeConstants.CUSTOMER_SERVICE_NOTE,customerServiceNote);
        Response<Boolean> response = orderWriteService.updateOrderExtra(shopOrderId,OrderLevel.SHOP,map);
        if (!response.isSuccess()){
            log.error("shopOrder add customerServiceNote failed,shopOrderId is({}),caused by{}",shopOrderId,response.getError());
            throw new JsonResponseException("add customer service note fail");
        }
    }
}

