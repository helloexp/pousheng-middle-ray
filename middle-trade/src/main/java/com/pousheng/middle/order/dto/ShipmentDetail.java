package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发货单详情
 * Created by songrenfei on 2017/6/22
 */
@Data
public class ShipmentDetail implements Serializable{

    private static final long serialVersionUID = -200347009366007210L;

    //发货信息
    private Shipment shipment;

    private OrderShipment orderShipment;

    private List<ShipmentItem> shipmentItems;

    //订单信息
    private ShopOrder shopOrder;

    /**
     * 用户收货地址信息
     */
    private ReceiverInfo  receiverInfo;

    /**
     * 发票信息
     */
    private List<Invoice> invoices;

    /**
     * 订单支付信息
     */
    private Payment payment;





}
