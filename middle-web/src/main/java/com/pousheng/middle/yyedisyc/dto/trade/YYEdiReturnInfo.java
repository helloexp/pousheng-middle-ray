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
    private int RefundChangeType;
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

    public String getSourceBillNo() {
        return SourceBillNo;
    }

    public void setSourceBillNo(String sourceBillNo) {
        SourceBillNo = sourceBillNo;
    }

    public String getShopBillNo() {
        return ShopBillNo;
    }

    public void setShopBillNo(String shopBillNo) {
        ShopBillNo = shopBillNo;
    }

    public String getBillType() {
        return BillType;
    }

    public void setBillType(String billType) {
        BillType = billType;
    }

    public String getShopCode() {
        return ShopCode;
    }

    public void setShopCode(String shopCode) {
        ShopCode = shopCode;
    }

    public String getShopName() {
        return ShopName;
    }

    public void setShopName(String shopName) {
        ShopName = shopName;
    }

    public String getBCMemberName() {
        return BCMemberName;
    }

    public void setBCMemberName(String BCMemberName) {
        this.BCMemberName = BCMemberName;
    }

    public String getStockCode() {
        return StockCode;
    }

    public void setStockCode(String stockCode) {
        StockCode = stockCode;
    }

    public String getCustomerCode() {
        return CustomerCode;
    }

    public void setCustomerCode(String customerCode) {
        CustomerCode = customerCode;
    }

    public String getCustomerName() {
        return CustomerName;
    }

    public void setCustomerName(String customerName) {
        CustomerName = customerName;
    }

    public String getExpressBillNo() {
        return ExpressBillNo;
    }

    public void setExpressBillNo(String expressBillNo) {
        ExpressBillNo = expressBillNo;
    }

    public int getIsRefundInvoice() {
        return IsRefundInvoice;
    }

    public void setIsRefundInvoice(int isRefundInvoice) {
        IsRefundInvoice = isRefundInvoice;
    }

    public int getRefundChangeType() {
        return RefundChangeType;
    }

    public void setRefundChangeType(int refundChangeType) {
        RefundChangeType = refundChangeType;
    }

    public BigDecimal getCollectionAmount() {
        return CollectionAmount;
    }

    public void setCollectionAmount(BigDecimal collectionAmount) {
        CollectionAmount = collectionAmount;
    }

    public BigDecimal getExpressAmount() {
        return ExpressAmount;
    }

    public void setExpressAmount(BigDecimal expressAmount) {
        ExpressAmount = expressAmount;
    }

    public int getFreightPay() {
        return FreightPay;
    }

    public void setFreightPay(int freightPay) {
        FreightPay = freightPay;
    }

    public String getSendContact() {
        return SendContact;
    }

    public void setSendContact(String sendContact) {
        SendContact = sendContact;
    }

    public String getSendContactTel() {
        return SendContactTel;
    }

    public void setSendContactTel(String sendContactTel) {
        SendContactTel = sendContactTel;
    }

    public String getSendProvince() {
        return SendProvince;
    }

    public void setSendProvince(String sendProvince) {
        SendProvince = sendProvince;
    }

    public String getSendCity() {
        return SendCity;
    }

    public void setSendCity(String sendCity) {
        SendCity = sendCity;
    }

    public String getSendArea() {
        return SendArea;
    }

    public void setSendArea(String sendArea) {
        SendArea = sendArea;
    }

    public String getSendAddress() {
        return SendAddress;
    }

    public void setSendAddress(String sendAddress) {
        SendAddress = sendAddress;
    }

    public String getZipCode() {
        return ZipCode;
    }

    public void setZipCode(String zipCode) {
        ZipCode = zipCode;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public int getExpectQty() {
        return ExpectQty;
    }

    public void setExpectQty(int expectQty) {
        ExpectQty = expectQty;
    }

    public int getTdq() {
        return Tdq;
    }

    public void setTdq(int tdq) {
        Tdq = tdq;
    }

    public Date getERPModifyTime() {
        return ERPModifyTime;
    }

    public void setERPModifyTime(Date ERPModifyTime) {
        this.ERPModifyTime = ERPModifyTime;
    }

    public List<YYEdiReturnItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiReturnItem> items) {
        this.items = items;
    }
}
