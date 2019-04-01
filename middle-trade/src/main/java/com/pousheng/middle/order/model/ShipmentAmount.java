package com.pousheng.middle.order.model;

import lombok.Data;

import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/2/7
 * pousheng-middle
 */
@Data
public class ShipmentAmount implements java.io.Serializable{

    private static final long serialVersionUID = -8847999716272413133L;
    private  Long id;
    private String orderNo;
    private String buyerNick;
    private String orderMon;
    private String feeMon;
    private String realMon;
    private String shopId;
    private String performanceShopId;
    private String stockId;
    private String onlineType;
    private String onlineOrderNo;
    private String orderSubNo;
    private String barCode;
    private String num;
    private String perferentialMon;
    private String hkOrderNo;
    private String posNo;
    private String salePrice;
    private String totalPrice;
    private Date createdAt;
    private Date updatedAt;
}
