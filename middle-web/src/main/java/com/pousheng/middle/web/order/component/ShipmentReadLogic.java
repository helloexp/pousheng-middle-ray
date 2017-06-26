package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @Autowired
    private ObjectMapper objectMapper;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;


    static final Integer BATCH_SIZE = 100;     // 批处理数量


    private final static TypeReference<List<ShipmentItem>> LIST_OF_SHIPMENT_ITEM =
            new TypeReference<List<ShipmentItem>>() {
            };

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 发货单详情
     */
    public ShipmentDetail orderDetail(Long orderShipmentId) {
        OrderShipment orderShipment = findOrderShipmentById(orderShipmentId);
        Shipment shipment = findShipmentById(orderShipment.getShipmentId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());

        ShipmentDetail shipmentDetail = new ShipmentDetail();
        shipmentDetail.setOrderShipment(orderShipment);
        shipmentDetail.setShipment(shipment);
        shipmentDetail.setShopOrder(shopOrder);
        shipmentDetail.setShipmentItems(getShipmentItems(shipment));
        shipmentDetail.setShipmentExtra(getShipmentExtra(shipment));
        setInvoiceInfo(shipmentDetail,orderShipment.getOrderId());
        setReceiverInfo(shipmentDetail,shipment);

        return shipmentDetail;

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


    /**
     * 商品详情返回发票信息
     */
    private void setInvoiceInfo(ShipmentDetail shipmentDetail, Long shopOrderId) {

        Response<List<Invoice>> invoicesRes = middleOrderReadService.findInvoiceInfo(shopOrderId,OrderLevel.SHOP);
        if(!invoicesRes.isSuccess()){
            log.error("failed to find order invoice, order id={}, order level:{} cause:{}",shopOrderId, OrderLevel.SHOP.getValue(), invoicesRes.getError());
            throw new JsonResponseException(invoicesRes.getError());
        }

        shipmentDetail.setInvoices(invoicesRes.getResult());
    }

    /**
     * 收货地址信息
     */
    private void setReceiverInfo(ShipmentDetail shipmentDetail,Shipment shipment) {
      /*  Response<List<OrderReceiverInfo>> orderReceiverInfoRes = middleOrderReadService.findOrderReceiverInfo(shopOrderId,OrderLevel.SHOP);
        if(!orderReceiverInfoRes.isSuccess()){
            log.error("find order receiver info by order id:{} order level:{} fai,cause:{}",shopOrderId,OrderLevel.SHOP.getValue(),orderReceiverInfoRes.getError());
            throw  new JsonResponseException(orderReceiverInfoRes.getError());
        }*/
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        shipmentDetail.setReceiverInfo(receiverInfo);
    }


    private List<ShipmentItem> getShipmentItems(Shipment shipment){
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


    private ShipmentExtra getShipmentExtra(Shipment shipment){
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
