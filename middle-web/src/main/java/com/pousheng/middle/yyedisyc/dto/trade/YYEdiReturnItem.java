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

    public int getRowNo() {
        return RowNo;
    }

    public void setRowNo(int rowNo) {
        RowNo = rowNo;
    }

    public String getCompanyCode() {
        return CompanyCode;
    }

    public void setCompanyCode(String companyCode) {
        CompanyCode = companyCode;
    }

    public String getBillNo() {
        return BillNo;
    }

    public void setBillNo(String billNo) {
        BillNo = billNo;
    }

    public String getSKU() {
        return SKU;
    }

    public void setSKU(String SKU) {
        this.SKU = SKU;
    }

    public String getMaterialCode() {
        return MaterialCode;
    }

    public void setMaterialCode(String materialCode) {
        MaterialCode = materialCode;
    }

    public String getSizeName() {
        return SizeName;
    }

    public void setSizeName(String sizeName) {
        SizeName = sizeName;
    }

    public String getExpectQty() {
        return ExpectQty;
    }

    public void setExpectQty(String expectQty) {
        ExpectQty = expectQty;
    }

    public String getShopBillNo() {
        return ShopBillNo;
    }

    public void setShopBillNo(String shopBillNo) {
        ShopBillNo = shopBillNo;
    }

    public BigDecimal getPayAmount() {
        return PayAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        PayAmount = payAmount;
    }

    public BigDecimal getRetailPrice() {
        return RetailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        RetailPrice = retailPrice;
    }

    public BigDecimal getBalaPrice() {
        return BalaPrice;
    }

    public void setBalaPrice(BigDecimal balaPrice) {
        BalaPrice = balaPrice;
    }
}
