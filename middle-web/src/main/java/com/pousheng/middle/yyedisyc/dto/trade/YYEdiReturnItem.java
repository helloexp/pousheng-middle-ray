package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiReturnItem implements java.io.Serializable {
    private static final long serialVersionUID = 3421289971277340109L;

    /**
     * 行号
     */
    @JsonProperty(value = "rowno")
    private int rowno;

    /**
     * 公司内码
     */
    @JsonProperty(value = "companycode")
    private String companycode;

    /**
     * ERP单号
     */
    @JsonProperty(value = "billno")
    private String billno;

    /**
     * 条码
     */
    @JsonProperty(value = "sku")
    private String sku;

    /**
     * 货号
     */
    @JsonProperty(value = "materialcode")
    private String materialcode;

    /**
     * 尺码名称
     */
    @JsonProperty(value = "sizename")
    private String sizename;

    /**
     * 预计数量
     */
    @JsonProperty(value = "expectqty")
    private int expectqty;

    /**
     * 网店交易单号
     */
    @JsonProperty(value = "shopbillno")
    private String shopbillno;

    /**
     * 结算金额(总价)
     */
    @JsonProperty(value = "payamount")
    private BigDecimal payamount;

    /**
     * 零售价
     */
    @JsonProperty(value = "retailprice")
    private BigDecimal retailprice;

    @JsonProperty(value = "edibillno")
    private String edibillno;

    /**
     * 结算价(单价)
     */
    @JsonProperty(value = "balaprice")
    private BigDecimal balaprice;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @JsonIgnore
    public int getRowno() {
        return rowno;
    }

    @JsonIgnore
    public void setRowno(int rowno) {
        this.rowno = rowno;
    }

    @JsonIgnore
    public String getCompanycode() {
        return companycode;
    }

    @JsonIgnore
    public void setCompanycode(String companycode) {
        this.companycode = companycode;
    }

    @JsonIgnore
    public String getBillno() {
        return billno;
    }

    @JsonIgnore
    public void setBillno(String billno) {
        this.billno = billno;
    }

    @JsonIgnore
    public String getSku() {
        return sku;
    }

    @JsonIgnore
    public void setSku(String sku) {
        this.sku = sku;
    }

    @JsonIgnore
    public String getMaterialcode() {
        return materialcode;
    }

    @JsonIgnore
    public void setMaterialcode(String materialcode) {
        this.materialcode = materialcode;
    }

    @JsonIgnore
    public String getSizename() {
        return sizename;
    }

    @JsonIgnore
    public void setSizename(String sizename) {
        this.sizename = sizename;
    }

    @JsonIgnore
    public int getExpectqty() {
        return expectqty;
    }

    @JsonIgnore
    public void setExpectqty(int expectqty) {
        this.expectqty = expectqty;
    }

    @JsonIgnore
    public String getShopbillno() {
        return shopbillno;
    }

    @JsonIgnore
    public void setShopbillno(String shopbillno) {
        this.shopbillno = shopbillno;
    }

    @JsonIgnore
    public BigDecimal getPayamount() {
        return payamount;
    }

    @JsonIgnore
    public void setPayamount(BigDecimal payamount) {
        this.payamount = payamount;
    }

    @JsonIgnore
    public BigDecimal getRetailprice() {
        return retailprice;
    }

    @JsonIgnore
    public void setRetailprice(BigDecimal retailprice) {
        this.retailprice = retailprice;
    }

    @JsonIgnore
    public String getEdibillno() {
        return edibillno;
    }

    @JsonIgnore
    public void setEdibillno(String edibillno) {
        this.edibillno = edibillno;
    }

    @JsonIgnore
    public BigDecimal getBalaprice() {
        return balaprice;
    }

    @JsonIgnore
    public void setBalaprice(BigDecimal balaprice) {
        this.balaprice = balaprice;
    }
}
