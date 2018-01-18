package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.format.annotation.DateTimeFormat;

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
    private String ERPModifyTime;

    @JsonProperty(value = "EDIBillNo")
    private String EDIBillNo;


    private List<YYEdiReturnItem> items;

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
    public String getSourceBillNo() {
        return SourceBillNo;
    }
    @JsonIgnore
    public void setSourceBillNo(String sourceBillNo) {
        SourceBillNo = sourceBillNo;
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
    public String getBillType() {
        return BillType;
    }
    @JsonIgnore
    public void setBillType(String billType) {
        BillType = billType;
    }
    @JsonIgnore
    public String getShopCode() {
        return ShopCode;
    }
    @JsonIgnore
    public void setShopCode(String shopCode) {
        ShopCode = shopCode;
    }
    @JsonIgnore
    public String getShopName() {
        return ShopName;
    }
    @JsonIgnore
    public void setShopName(String shopName) {
        ShopName = shopName;
    }
    @JsonIgnore
    public String getBCMemberName() {
        return BCMemberName;
    }
    @JsonIgnore
    public void setBCMemberName(String BCMemberName) {
        this.BCMemberName = BCMemberName;
    }
    @JsonIgnore
    public String getStockCode() {
        return StockCode;
    }
    @JsonIgnore
    public void setStockCode(String stockCode) {
        StockCode = stockCode;
    }
    @JsonIgnore
    public String getCustomerCode() {
        return CustomerCode;
    }
    @JsonIgnore
    public void setCustomerCode(String customerCode) {
        CustomerCode = customerCode;
    }
    @JsonIgnore
    public String getCustomerName() {
        return CustomerName;
    }
    @JsonIgnore
    public void setCustomerName(String customerName) {
        CustomerName = customerName;
    }
    @JsonIgnore
    public String getExpressBillNo() {
        return ExpressBillNo;
    }
    @JsonIgnore
    public void setExpressBillNo(String expressBillNo) {
        ExpressBillNo = expressBillNo;
    }
    @JsonIgnore
    public int getIsRefundInvoice() {
        return IsRefundInvoice;
    }
    @JsonIgnore
    public void setIsRefundInvoice(int isRefundInvoice) {
        IsRefundInvoice = isRefundInvoice;
    }
    @JsonIgnore
    public int getRefundChangeType() {
        return RefundChangeType;
    }
    @JsonIgnore
    public void setRefundChangeType(int refundChangeType) {
        RefundChangeType = refundChangeType;
    }
    @JsonIgnore
    public BigDecimal getCollectionAmount() {
        return CollectionAmount;
    }
    @JsonIgnore
    public void setCollectionAmount(BigDecimal collectionAmount) {
        CollectionAmount = collectionAmount;
    }
    @JsonIgnore
    public BigDecimal getExpressAmount() {
        return ExpressAmount;
    }
    @JsonIgnore
    public void setExpressAmount(BigDecimal expressAmount) {
        ExpressAmount = expressAmount;
    }
    @JsonIgnore
    public int getFreightPay() {
        return FreightPay;
    }
    @JsonIgnore
    public void setFreightPay(int freightPay) {
        FreightPay = freightPay;
    }
    @JsonIgnore
    public String getSendContact() {
        return SendContact;
    }
    @JsonIgnore
    public void setSendContact(String sendContact) {
        SendContact = sendContact;
    }
    @JsonIgnore
    public String getSendContactTel() {
        return SendContactTel;
    }
    @JsonIgnore
    public void setSendContactTel(String sendContactTel) {
        SendContactTel = sendContactTel;
    }
    @JsonIgnore
    public String getSendProvince() {
        return SendProvince;
    }
    @JsonIgnore
    public void setSendProvince(String sendProvince) {
        SendProvince = sendProvince;
    }
    @JsonIgnore
    public String getSendCity() {
        return SendCity;
    }
    @JsonIgnore
    public void setSendCity(String sendCity) {
        SendCity = sendCity;
    }
    @JsonIgnore
    public String getSendArea() {
        return SendArea;
    }
    @JsonIgnore
    public void setSendArea(String sendArea) {
        SendArea = sendArea;
    }
    @JsonIgnore
    public String getSendAddress() {
        return SendAddress;
    }
    @JsonIgnore
    public void setSendAddress(String sendAddress) {
        SendAddress = sendAddress;
    }
    @JsonIgnore
    public String getZipCode() {
        return ZipCode;
    }
    @JsonIgnore
    public void setZipCode(String zipCode) {
        ZipCode = zipCode;
    }
    @JsonIgnore
    public String getAddress() {
        return Address;
    }
    @JsonIgnore
    public void setAddress(String address) {
        Address = address;
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
    public int getTdq() {
        return Tdq;
    }
    @JsonIgnore
    public void setTdq(int tdq) {
        Tdq = tdq;
    }
    @JsonIgnore
    public String getERPModifyTime() {
        return ERPModifyTime;
    }
    @JsonIgnore
    public void setERPModifyTime(String ERPModifyTime) {
        this.ERPModifyTime = ERPModifyTime;
    }

    public List<YYEdiReturnItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiReturnItem> items) {
        this.items = items;
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
