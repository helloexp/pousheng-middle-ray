package com.pousheng.middle.hksyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */

public class SycHkShipmentOrder implements Serializable {

    private static final long serialVersionUID = 3785578697983248329L;

    private String orderNo;
    private String buyerNick;
    private BigDecimal orderMon;
    private BigDecimal feeMon;
    private BigDecimal realMon;
    private String buyerRemark;
    private String paymentSerialNo;
    private String orderStatus;
    private String createdDate;
    private String updatedDate;
    private String PaymentType;
    private String invoiceType;
    private String Invoice;
    private String taxNo;
    private String shopId;
    private String performanceShopId;
    private String stockId;
    @JsonProperty(value = "VendCustCode")
    private String VendCustCode="";
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

    public BigDecimal getOrderMon() {
        return orderMon;
    }

    public void setOrderMon(BigDecimal orderMon) {
        this.orderMon = orderMon;
    }

    public BigDecimal getFeeMon() {
        return feeMon;
    }

    public void setFeeMon(BigDecimal feeMon) {
        this.feeMon = feeMon;
    }

    public BigDecimal getRealMon() {
        return realMon;
    }

    public void setRealMon(BigDecimal realMon) {
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
    public String getVendCustCode() {
        return VendCustCode;
    }
    @JsonIgnore
    public void setVendCustCode(String vendCustCode) {
        VendCustCode = vendCustCode;
    }

    public List<SycHkShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<SycHkShipmentItem> items) {
        this.items = items;
    }
}
