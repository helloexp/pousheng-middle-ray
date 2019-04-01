package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefund implements Serializable{

    private static final long serialVersionUID = 2097985020187809700L;

    private String refundNo;
    private String orderNo;
    private String shopId;
    private String shopName;
    private String stockId;
    private String performanceShopId;
    private String refundOrderAmount;
    private Integer refundFreight;
    private String type;
    private String status;
    private String createdDate;
    private String totalRefund;
    private String logisticsCompany;
    private String logisticsCode;
    private String memo;
    private String onlineType;
    private Integer isRefused;

}
