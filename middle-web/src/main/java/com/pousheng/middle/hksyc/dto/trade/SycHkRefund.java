package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefund implements Serializable{

    private static final long serialVersionUID = 2097985020187809700L;

    private String refundNo = "r00001";
    private String orderNo = "o00001";
    private String shopId = "hkh01";
    private String stockId = "hk00001";
    private String performanceShopId = "hkh01";
    private Integer refundOrderAmount = 201;
    private Integer refundFreight = 10;
    private String type = "0";
    private String status = "4";
    private String createdDate = "2017-10-01 00:00:00";
    private Integer totalRefund = 211;
    private String logisticsCompany = "";
    private String logisticsCode = "";
    private String memo = "不想要了。";
}
