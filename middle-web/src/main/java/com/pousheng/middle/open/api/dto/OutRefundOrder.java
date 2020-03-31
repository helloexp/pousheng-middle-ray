package com.pousheng.middle.open.api.dto;

import com.pousheng.middle.order.enums.MiddleChannel;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/4/1
 */
@Data
public class OutRefundOrder implements Serializable{

    private static final long serialVersionUID = 5290900159956035367L;


    private String outOrderId; //订单id
    private String outAfterSaleOrderId;//售后单id
    private String buyerName;
    private Long fee;
    private Integer type;//1: 售后仅退款，2: 售后退货退款 （默认传2） 6拒收
    private String buyerMobile;
    private String buyerNote;//买家备注
    private String sellerNote;//商家备注
    private Integer status;//默认传1已同意
    private String expressCode;//寄回的快递单号
    private String applyAt; //申请时间 yyyyMMdd HHmmss
    private String returnStockid;//云聚传过来为空则默认退货仓规则， 非空则以云聚值覆盖
    private String channel = MiddleChannel.YUNJUBBC.getValue();






}
