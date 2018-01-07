package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiReturnItem implements java.io.Serializable{
    private static final long serialVersionUID = 3421289971277340109L;

    /**
     *行号
     */
    @JsonProperty(value = "RowNo")
    private int RowNo;

    /**
     * 公司内码
     */
    @JsonProperty(value = "CompanyCode")
    private String CompanyCode;

    /**
     * ERP单号
     */
    @JsonProperty(value = "BillNo")
    private String BillNo;

    /**
     * 条码
     */
    @JsonProperty(value = "SKU")
    private String SKU;

    /**
     * 货号
     */
    @JsonProperty(value = "MaterialCode")
    private String MaterialCode;

    /**
     * 尺码名称
     */
    @JsonProperty(value = "SizeName")
    private String SizeName;

    /**
     * 预计数量
     */
    @JsonProperty(value = "ExpectQty")
    private String ExpectQty;

    /**
     * 网店交易单号
     */
    @JsonProperty(value = "ShopBillNo")
    private String ShopBillNo;

    /**
     * 结算金额(总价)
     */
    @JsonProperty(value = "PayAmount")
    private BigDecimal PayAmount;

    /**
     * 零售价
     */
    @JsonProperty(value = "RetailPrice")
    private BigDecimal RetailPrice;

    /**
     * 结算价(单价)
     */
    @JsonProperty(value = "BalaPrice")
    private BigDecimal BalaPrice;
}
