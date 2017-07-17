package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class ShipmentReadLogic {

    @Autowired
    private FlowPicker flowPicker;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ObjectMapper objectMapper;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 发货单详情
     */
    public ShipmentDetail orderDetail(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);
        OrderShipment orderShipment = findOrderShipmentByShipmentId(shipmentId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());

        ShipmentDetail shipmentDetail = new ShipmentDetail();
        shipmentDetail.setOrderShipment(orderShipment);
        shipmentDetail.setShipment(shipment);
        shipmentDetail.setShopOrder(shopOrder);
        shipmentDetail.setShipmentItems(getShipmentItems(shipment));
        shipmentDetail.setShipmentExtra(getShipmentExtra(shipment));
        setInvoiceInfo(shipmentDetail,orderShipment.getOrderId());
        setReceiverInfo(shipmentDetail,shipment);
        List<Payment> payments = orderReadLogic.findOrderPaymentInfo(orderShipment.getOrderId());
        if(!CollectionUtils.isEmpty(payments)){
            shipmentDetail.setPayment(payments.get(0));
        }

        return shipmentDetail;

    }



    public Response<ShipmentPreview> orderShipPreview(Long shopOrderId, String data){
        Map<Long, Integer> skuOrderIdAndQuantity = analysisSkuOrderIdAndQuantity(data);

        Response<OrderDetail> orderDetailRes = orderReadLogic.orderDetail(shopOrderId);
        if(!orderDetailRes.isSuccess()){
            log.error("find order detail by order id:{} fail,error:{}",shopOrderId,orderDetailRes.getError());
            throw new JsonResponseException(orderDetailRes.getError());
        }
        OrderDetail orderDetail = orderDetailRes.getResult();
        List<SkuOrder> allSkuOrders = orderDetail.getSkuOrders();
        List<SkuOrder> currentSkuOrders = allSkuOrders.stream().filter(skuOrder -> skuOrderIdAndQuantity.containsKey(skuOrder.getId())).collect(Collectors.toList());
        currentSkuOrders.forEach(skuOrder -> skuOrder.setQuantity(skuOrderIdAndQuantity.get(skuOrder.getId())));


        //封装发货预览基本信息
        ShipmentPreview shipmentPreview  = new ShipmentPreview();

        //todo 绩效店铺名称
        shipmentPreview.setErpPerformanceShopName("绩效店铺");
        //绩效店铺编码
        shipmentPreview.setErpPerformanceShopCode("TEST001");
        //下单店铺名称
        shipmentPreview.setErpOrderShopName("下单店铺");
        //下单店铺编码
        shipmentPreview.setErpOrderShopCode("TEST002");
        shipmentPreview.setInvoices(orderDetail.getInvoices());
        shipmentPreview.setPayment(orderDetail.getPayment());
        List<OrderReceiverInfo> orderReceiverInfos = orderDetail.getOrderReceiverInfos();
        shipmentPreview.setReceiverInfo(JsonMapper.nonDefaultMapper().fromJson(orderReceiverInfos.get(0).getReceiverInfoJson(),ReceiverInfo.class));
        shipmentPreview.setShopOrder(orderDetail.getShopOrder());
        //封装发货预览商品信息
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithCapacity(currentSkuOrders.size());
        for (SkuOrder skuOrder : currentSkuOrders){
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setSkuOrderId(skuOrder.getId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setQuantity(skuOrder.getQuantity());
            //todo 计算各种价格
            shipmentItem.setCleanFee(10000);

            shipmentItems.add(shipmentItem);
        }
        shipmentPreview.setShipmentItems(shipmentItems);

        return Response.ok(shipmentPreview);
    }




    public Response<ShipmentPreview> changeShipPreview(Long refundId,String data){
        Map<String, Integer> skuCodeAndQuantity = analysisSkuCodeAndQuantity(data);
        Refund refund = refundReadLogic.findRefundById(refundId);
        List<RefundItem>  refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);

        //订单基本信息
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());//orderRefund.getOrderId()为交易订单id
        List<Invoice> invoices = orderReadLogic.findInvoiceInfo(orderRefund.getOrderId());
        List<Payment> payments = orderReadLogic.findOrderPaymentInfo(orderRefund.getOrderId());
        ReceiverInfo receiverInfo = orderReadLogic.findReceiverInfo(orderRefund.getOrderId());


        //封装发货预览基本信息
        ShipmentPreview shipmentPreview  = new ShipmentPreview();
        shipmentPreview.setInvoices(invoices);
        if(!CollectionUtils.isEmpty(payments)){
            shipmentPreview.setPayment(payments.get(0));
        }
        shipmentPreview.setReceiverInfo(receiverInfo);
        shipmentPreview.setShopOrder(shopOrder);
        //封装发货预览商品信息
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithCapacity(refundChangeItems.size());
        for (RefundItem refundItem : refundChangeItems){
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setSkuCode(refundItem.getSkuCode());
            shipmentItem.setOutSkuCode(refundItem.getOutSkuCode());
            shipmentItem.setSkuName(refundItem.getSkuName());
            shipmentItem.setQuantity(skuCodeAndQuantity.get(refundItem.getSkuCode()));//替换为发货数量
            //todo 计算各种价格

            shipmentItems.add(shipmentItem);
        }
        shipmentPreview.setShipmentItems(shipmentItems);

        return Response.ok(shipmentPreview);
    }

    public List<OrderShipment> findByOrderIdAndType(Long orderId){
        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order shipment by order id:{} level:{} fail,error:{}",orderId,OrderLevel.SHOP.toString(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }

    private Map<Long, Integer> analysisSkuOrderIdAndQuantity(String data){
        Map<Long, Integer> skuOrderIdAndQuantity = mapper.fromJson(data, mapper.createCollectionType(HashMap.class, Long.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuOrderIdAndQuantity:{}",data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }

    private Map<String, Integer> analysisSkuCodeAndQuantity(String data){
        Map<String, Integer> skuOrderIdAndQuantity = mapper.fromJson(data, mapper.createCollectionType(HashMap.class, String.class, Integer.class));
        if(skuOrderIdAndQuantity == null) {
            log.error("failed to parse skuCodeAndQuantity:{}",data);
            throw new JsonResponseException("sku.applyQuantity.invalid");
        }
        return skuOrderIdAndQuantity;
    }





    public List<OrderShipment> findByAfterOrderIdAndType(Long afterSaleOrderId){
        Response<List<OrderShipment>> response = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId ,OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order shipment by order id:{} level:{} fail,error:{}",afterSaleOrderId,OrderLevel.SHOP.toString(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();

    }



    public Shipment findShipmentById(Long shipmentId){
        Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
        if(!shipmentRes.isSuccess()){
            log.error("find shipment by id:{} fail,error:{}",shipmentId,shipmentRes.getError());
            throw new JsonResponseException(shipmentRes.getError());
        }
        return shipmentRes.getResult();
    }

    public OrderShipment findOrderShipmentById(Long orderShipmentId){
        Response<OrderShipment> orderShipmentRes = orderShipmentReadService.findById(orderShipmentId);
        if(!orderShipmentRes.isSuccess()){
            log.error("find order shipment by id:{} fail,error:{}",orderShipmentId,orderShipmentRes.getError());
            throw new JsonResponseException(orderShipmentRes.getError());
        }

        return orderShipmentRes.getResult();
    }

    public OrderShipment findOrderShipmentByShipmentId(Long shipmenId){
        Response<OrderShipment> orderShipmentRes = orderShipmentReadService.findByShipmentId(shipmenId);
        if(!orderShipmentRes.isSuccess()){
            log.error("find order shipment by shipment id:{} fail,error:{}",shipmenId,orderShipmentRes.getError());
            throw new JsonResponseException(orderShipmentRes.getError());
        }

        return orderShipmentRes.getResult();
    }



    /**
     * 商品详情返回发票信息
     */
    private void setInvoiceInfo(ShipmentDetail shipmentDetail, Long shopOrderId) {

        shipmentDetail.setInvoices(orderReadLogic.findInvoiceInfo(shopOrderId));
    }

    /**
     * 收货地址信息
     */
    private void setReceiverInfo(ShipmentDetail shipmentDetail,Shipment shipment) {
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        shipmentDetail.setReceiverInfo(receiverInfo);
    }


    public List<ShipmentItem> getShipmentItems(Shipment shipment){
        Map<String,String> extraMap = shipment.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("shipment(id:{}) extra field is null",shipment.getId());
            throw new JsonResponseException("shipment.extra.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.SHIPMENT_ITEM_INFO)){
            log.error("shipment(id:{}) extra not contain key:{}",shipment.getId(),TradeConstants.SHIPMENT_ITEM_INFO);
            throw new JsonResponseException("shipment.extra.item.info.null");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.SHIPMENT_ITEM_INFO),mapper.createCollectionType(List.class,ShipmentItem.class));
    }



    public ShipmentExtra getShipmentExtra(Shipment shipment){
        Map<String,String> extraMap = shipment.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("shipment(id:{}) extra field is null",shipment.getId());
            throw new JsonResponseException("shipment.extra.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.SHIPMENT_EXTRA_INFO)){
            log.error("shipment(id:{}) extra not contain key:{}",shipment.getId(),TradeConstants.SHIPMENT_EXTRA_INFO);
            throw new JsonResponseException("shipment.extra.extra.info.null");
        }

        return mapper.fromJson(extraMap.get(TradeConstants.SHIPMENT_EXTRA_INFO),ShipmentExtra.class);


    }


}
