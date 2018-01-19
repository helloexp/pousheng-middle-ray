package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private int ExpectQty;

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

    @JsonProperty(value = "EDIBillNo")
    private String EDIBillNo;

    /**
     * 结算价(单价)
     */
    @JsonProperty(value = "BalaPrice")
    private BigDecimal BalaPrice;
    @JsonIgnore
    public int getRowNo() {
        return RowNo;
    }
    @JsonIgnore
    public void setRowNo(int rowNo) {
        RowNo = rowNo;
    }
    @JsonIgnore
    public String getCompanyCode() {
        return CompanyCode;
    }
    @JsonIgnore
    public void setCompanyCode(String companyCode) {
        CompanyCode = companyCode;
    }
    @JsonIgnore
    public String getBillNo() {
        return BillNo;
    }
    @JsonIgnore
    public void setBillNo(String billNo) {
        BillNo = billNo;
    }
    @JsonIgnore
    public String getSKU() {
        return SKU;
    }
    @JsonIgnore
    public void setSKU(String SKU) {
        this.SKU = SKU;
    }
    @JsonIgnore
    public String getMaterialCode() {
        return MaterialCode;
    }
    @JsonIgnore
    public void setMaterialCode(String materialCode) {
        MaterialCode = materialCode;
    }
    @JsonIgnore
    public String getSizeName() {
        return SizeName;
    }
    @JsonIgnore
    public void setSizeName(String sizeName) {
        SizeName = sizeName;
    }
    @JsonIgnore
    public int getExpectQty() {
        return ExpectQty;
    }
    @JsonIgnore
    public void setExpectQty(int expectQty) {
        ExpectQty = expectQty;
    }
    @JsonIgnore
    public String getShopBillNo() {
        return ShopBillNo;
    }
    @JsonIgnore
    public void setShopBillNo(String shopBillNo) {
        ShopBillNo = shopBillNo;
    }
    @JsonIgnore
    public BigDecimal getPayAmount() {
        return PayAmount;
    }
    @JsonIgnore
    public void setPayAmount(BigDecimal payAmount) {
        PayAmount = payAmount;
    }
    @JsonIgnore
    public BigDecimal getRetailPrice() {
        return RetailPrice;
    }
    @JsonIgnore
    public void setRetailPrice(BigDecimal retailPrice) {
        RetailPrice = retailPrice;
    }
    @JsonIgnore
    public BigDecimal getBalaPrice() {
        return BalaPrice;
    }
    @JsonIgnore
    public void setBalaPrice(BigDecimal balaPrice) {
        BalaPrice = balaPrice;
    }
    @JsonIgnore
    public String getEDIBillNo() {
        return EDIBillNo;
    }
    @JsonIgnore
    public void setEDIBillNo(String EDIBillNo) {
        this.EDIBillNo = EDIBillNo;
    }
}
