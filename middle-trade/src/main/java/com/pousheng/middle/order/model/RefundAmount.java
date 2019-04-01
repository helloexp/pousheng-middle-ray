package com.pousheng.middle.order.model;

import lombok.Data;

import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/2/7
 * pousheng-middle
 */
@Data
public class RefundAmount implements java.io.Serializable{

    private static final long serialVersionUID = 6124494172238966693L;
    private  Long id;
    private String refundNo;
    private String orderNo;
    private String shopId;
    private String performanceShopId;
    private String stockId;
    private String refundOrderAmount;
    private String type;
    private String totalRefund;
    private String onlineOrderNo;
    private String hkOrderNo;
    private String posNo;
    private String refundSubNo;
    private String orderSubNo;
    private String barCode;
    private String itemNum;
    private String salePrice;
    private String refundAmount;
    private Date createdAt;
    private Date updatedAt;
}
