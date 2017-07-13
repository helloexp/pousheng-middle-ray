package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.Payment;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 交易订单基本信息
 * Created by songrenfei on 2017/7/1
 */
@Data
public class OrderBasicInfo implements Serializable{

    private static final long serialVersionUID = 3994041460159737291L;


    /**
     * 订单信息
     */
    private ShopOrder shopOrder;

    /**
     * 用户收货地址信息
     */
    private ReceiverInfo receiverInfo;

    /**
     * 发票信息
     */
    private List<Invoice> invoices;

    /**
     * 订单支付信息
     */
    private Payment payment;

}
