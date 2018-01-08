package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiReturnInfo implements java.io.Serializable{
    private static final long serialVersionUID = 2800107524148782503L;
    @JsonProperty(value = "CompanyCode")
    private String CompanyCode;
    @JsonProperty(value = "BillNo")
    private String BillNo;
    @JsonProperty(value = "SourceBillNo")
    private String SourceBillNo;
    @JsonProperty(value = "ShopBillNo")
    private String ShopBillNo;
    @JsonProperty(value = "BillType")
    private String BillType;
    @JsonProperty(value = "ShopCode")
    private String ShopCode;
    @JsonProperty(value = "ShopName")
    private String ShopName;
    @JsonProperty(value = "BCMemberName")
    private String BCMemberName;
    @JsonProperty(value = "StockCode")
    private String StockCode;
    @JsonProperty(value = "CustomerCode")
    private String CustomerCode;
    @JsonProperty(value = "CustomerName")
    private String CustomerName;
    @JsonProperty(value = "ExpressBillNo")
    private String ExpressBillNo;
    @JsonProperty(value = "IsRefundInvoice")
    private int IsRefundInvoice;
    @JsonProperty(value = "RefundChangeType")
    private String RefundChangeType;
    @JsonProperty(value = "CollectionAmount")
    private BigDecimal CollectionAmount;
    @JsonProperty(value = "ExpressAmount")
    private BigDecimal ExpressAmount;
    @JsonProperty(value = "FreightPay")
    private int FreightPay;
    @JsonProperty(value = "SendContact")
    private String SendContact;
    @JsonProperty(value = "SendContactTel")
    private String SendContactTel;
    @JsonProperty(value = "SendProvince")
    private String SendProvince;
    @JsonProperty(value = "SendCity")
    private String SendCity;
    @JsonProperty(value = "SendArea")
    private String SendArea;
    @JsonProperty(value = "SendAddress")
    private String SendAddress;
    @JsonProperty(value = "ZipCode")
    private String ZipCode;
    @JsonProperty(value = "Address")
    private String Address;
    @JsonProperty(value = "ExpectQty")
    private int ExpectQty;
    @JsonProperty(value = "Tdq")
    private int Tdq;
    @JsonProperty(value = "ERPModifyTime")
    private Date ERPModifyTime;

    private List<YYEdiReturnItem> items;
}
