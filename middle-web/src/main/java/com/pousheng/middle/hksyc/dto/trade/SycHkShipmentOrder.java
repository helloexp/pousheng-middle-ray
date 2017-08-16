package com.pousheng.middle.hksyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */

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
    @JsonProperty(value = "VendCustID")
    private String VendCustID="";
    private List<SycHkShipmentItem> items;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBuyerNick() {
        return buyerNick;
    }

    public void setBuyerNick(String buyerNick) {
        this.buyerNick = buyerNick;
    }

    public Integer getOrderMon() {
        return orderMon;
    }

    public void setOrderMon(Integer orderMon) {
        this.orderMon = orderMon;
    }

    public Integer getFeeMon() {
        return feeMon;
    }

    public void setFeeMon(Integer feeMon) {
        this.feeMon = feeMon;
    }

    public Integer getRealMon() {
        return realMon;
    }

    public void setRealMon(Integer realMon) {
        this.realMon = realMon;
    }

    public String getBuyerRemark() {
        return buyerRemark;
    }

    public void setBuyerRemark(String buyerRemark) {
        this.buyerRemark = buyerRemark;
    }

    public String getPaymentSerialNo() {
        return paymentSerialNo;
    }

    public void setPaymentSerialNo(String paymentSerialNo) {
        this.paymentSerialNo = paymentSerialNo;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getPaymentType() {
        return PaymentType;
    }

    public void setPaymentType(String paymentType) {
        PaymentType = paymentType;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoice() {
        return Invoice;
    }

    public void setInvoice(String invoice) {
        Invoice = invoice;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getPerformanceShopId() {
        return performanceShopId;
    }

    public void setPerformanceShopId(String performanceShopId) {
        this.performanceShopId = performanceShopId;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }
    @JsonIgnore
    public String getVendCustID() {
        return VendCustID;
    }
    @JsonIgnore
    public void setVendCustID(String vendCustID) {
        VendCustID = vendCustID;
    }

    public List<SycHkShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<SycHkShipmentItem> items) {
        this.items = items;
    }
}
