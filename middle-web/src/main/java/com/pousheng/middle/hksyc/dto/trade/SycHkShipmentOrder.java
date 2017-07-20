package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentOrder implements Serializable {

    private static final long serialVersionUID = 3785578697983248329L;

    private String orderNo = "o00001";
    private String buyerNick = "tom";
    private Integer orderMon = 199;
    private Integer feeMon = 10;
    private Integer realMon = 211;
    private String buyerRemark = "优先给我发货";
    private String paymentSerialNo = "2017010121244401";
    private String orderStatus = "2";
    private String createdDate = "2017-10-01 00:00:00";
    private String updatedDate = "2017-01-01 00:00:01";
    private String PaymentType = "0";
    private String invoiceType = "1";
    private String Invoice = "广州XX公司";
    private String taxNo = "123456789";
    private String shopId = "hkh01";
    private String performanceShopId = "hkh01";
    private String stockId = "hkh007";

    private List<SycHkShipmentItem> items;

}
