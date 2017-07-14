package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 发货单扩展信息
 * Created by songrenfei on 2017/6/26
 */
@Data
public class ShipmentExtra implements Serializable{

    private static final long serialVersionUID = 1970760321501280133L;

    //发货仓ID
    private Long warehouseId;

    //发货仓名称
    private String warehouseName;

    //绩效店铺名称
    private String erpPerformanceShopName;
    //绩效店铺编码
    private String erpPerformanceShopCode;
    //下单店铺名称
    private String erpOrderShopName;
    //下单店铺编码
    private String erpOrderShopCode;

    //发货单商品金额
    private Long shipmentItemFee;
    //发货单运费金额
    private Long shipmentShipFee;
    //发货单运费优惠金额
    private Long shipmentShipDiscountFee;
    //发货单优惠金额
    private Long shipmentDiscountFee;
    //发货单优惠金额
    private Long shipmentTotalFee;
    //ERP发货单号
    private String outShipmentId;
    //物流公司代码
    private String shipmentCorpCode;
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;
    //发货时间
    private Date shipmentDate;

}