package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
public class YYEdiReturnInfo implements java.io.Serializable {
    private static final long serialVersionUID = 2800107524148782503L;
    @JsonProperty(value = "companycode")
    private String companycode;
    @JsonProperty(value = "billno")
    private String billno;
    @JsonProperty(value = "sourcebillno")
    private String sourcebillno;
    @JsonProperty(value = "shopbillno")
    private String shopbillno;
    @JsonProperty(value = "billtype")
    private String billtype;
    @JsonProperty(value = "channel")
    private String channel;
    @JsonProperty(value = "shopcode")
    private String shopcode;
    @JsonProperty(value = "shopname")
    private String shopname;
    @JsonProperty(value = "bcmembername")
    private String bcmembername;
    @JsonProperty(value = "stockcode")
    private String stockcode;
    @JsonProperty(value = "customercode")
    private String customercode;
    @JsonProperty(value = "customername")
    private String customername;
    @JsonProperty(value = "expressbillno")
    private String expressbillno;
    @JsonProperty(value = "isrefundinvoice")
    private int isrefundinvoice;
    @JsonProperty(value = "refundchangetype")
    private int refundchangetype;
    @JsonProperty(value = "collectionamount")
    private BigDecimal collectionamount;
    @JsonProperty(value = "expressamount")
    private BigDecimal expressamount;
    @JsonProperty(value = "freightpay")
    private int freightpay;
    @JsonProperty(value = "sendcontact")
    private String sendcontact;
    @JsonProperty(value = "sendcontacttel")
    private String sendcontacttel;
    @JsonProperty(value = "sendprovince")
    private String sendprovince;
    @JsonProperty(value = "sendcity")
    private String sendcity;
    @JsonProperty(value = "sendarea")
    private String sendarea;
    @JsonProperty(value = "sendaddress")
    private String sendaddress;
    @JsonProperty(value = "zipcode")
    private String zipcode;
    @JsonProperty(value = "address")
    private String address;
    @JsonProperty(value = "expectqty")
    private int expectqty;
    @JsonProperty(value = "tdq")
    private int tdq;
    @JsonProperty(value = "erpmodifytime")
    private String erpmodifytime;

    @JsonProperty(value = "edibillno")
    private String edibillno;

    private List<YYEdiReturnItem> items;

    public static long getSerialVersionUID() {
        return serialVersionUID;
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
    public String getSourcebillno() {
        return sourcebillno;
    }
    @JsonIgnore
    public void setSourcebillno(String sourcebillno) {
        this.sourcebillno = sourcebillno;
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
    public String getBilltype() {
        return billtype;
    }
    @JsonIgnore
    public void setBilltype(String billtype) {
        this.billtype = billtype;
    }
    @JsonIgnore
    public String getChannel() {
        return channel;
    }
    @JsonIgnore
    public void setChannel(String channel) {
        this.channel = channel;
    }
    @JsonIgnore
    public String getShopcode() {
        return shopcode;
    }
    @JsonIgnore
    public void setShopcode(String shopcode) {
        this.shopcode = shopcode;
    }
    @JsonIgnore
    public String getShopname() {
        return shopname;
    }
    @JsonIgnore
    public void setShopname(String shopname) {
        this.shopname = shopname;
    }
    @JsonIgnore
    public String getBcmembername() {
        return bcmembername;
    }
    @JsonIgnore
    public void setBcmembername(String bcmembername) {
        this.bcmembername = bcmembername;
    }
    @JsonIgnore
    public String getStockcode() {
        return stockcode;
    }
    @JsonIgnore
    public void setStockcode(String stockcode) {
        this.stockcode = stockcode;
    }
    @JsonIgnore
    public String getCustomercode() {
        return customercode;
    }
    @JsonIgnore
    public void setCustomercode(String customercode) {
        this.customercode = customercode;
    }
    @JsonIgnore
    public String getCustomername() {
        return customername;
    }
    @JsonIgnore
    public void setCustomername(String customername) {
        this.customername = customername;
    }
    @JsonIgnore
    public String getExpressbillno() {
        return expressbillno;
    }
    @JsonIgnore
    public void setExpressbillno(String expressbillno) {
        this.expressbillno = expressbillno;
    }
    @JsonIgnore
    public int getIsrefundinvoice() {
        return isrefundinvoice;
    }
    @JsonIgnore
    public void setIsrefundinvoice(int isrefundinvoice) {
        this.isrefundinvoice = isrefundinvoice;
    }
    @JsonIgnore
    public int getRefundchangetype() {
        return refundchangetype;
    }
    @JsonIgnore
    public void setRefundchangetype(int refundchangetype) {
        this.refundchangetype = refundchangetype;
    }
    @JsonIgnore
    public BigDecimal getCollectionamount() {
        return collectionamount;
    }
    @JsonIgnore
    public void setCollectionamount(BigDecimal collectionamount) {
        this.collectionamount = collectionamount;
    }
    @JsonIgnore
    public BigDecimal getExpressamount() {
        return expressamount;
    }
    @JsonIgnore
    public void setExpressamount(BigDecimal expressamount) {
        this.expressamount = expressamount;
    }
    @JsonIgnore
    public int getFreightpay() {
        return freightpay;
    }
    @JsonIgnore
    public void setFreightpay(int freightpay) {
        this.freightpay = freightpay;
    }
    @JsonIgnore
    public String getSendcontact() {
        return sendcontact;
    }
    @JsonIgnore
    public void setSendcontact(String sendcontact) {
        this.sendcontact = sendcontact;
    }
    @JsonIgnore
    public String getSendcontacttel() {
        return sendcontacttel;
    }
    @JsonIgnore
    public void setSendcontacttel(String sendcontacttel) {
        this.sendcontacttel = sendcontacttel;
    }
    @JsonIgnore
    public String getSendprovince() {
        return sendprovince;
    }
    @JsonIgnore
    public void setSendprovince(String sendprovince) {
        this.sendprovince = sendprovince;
    }
    @JsonIgnore
    public String getSendcity() {
        return sendcity;
    }
    @JsonIgnore
    public void setSendcity(String sendcity) {
        this.sendcity = sendcity;
    }
    @JsonIgnore
    public String getSendarea() {
        return sendarea;
    }
    @JsonIgnore
    public void setSendarea(String sendarea) {
        this.sendarea = sendarea;
    }
    @JsonIgnore
    public String getSendaddress() {
        return sendaddress;
    }
    @JsonIgnore
    public void setSendaddress(String sendaddress) {
        this.sendaddress = sendaddress;
    }
    @JsonIgnore
    public String getZipcode() {
        return zipcode;
    }
    @JsonIgnore
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }
    @JsonIgnore
    public String getAddress() {
        return address;
    }
    @JsonIgnore
    public void setAddress(String address) {
        this.address = address;
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
    public int getTdq() {
        return tdq;
    }
    @JsonIgnore
    public void setTdq(int tdq) {
        this.tdq = tdq;
    }
    @JsonIgnore
    public String getErpmodifytime() {
        return erpmodifytime;
    }
    @JsonIgnore
    public void setErpmodifytime(String erpmodifytime) {
        this.erpmodifytime = erpmodifytime;
    }
    @JsonIgnore
    public String getEdibillno() {
        return edibillno;
    }
    @JsonIgnore
    public void setEdibillno(String edibillno) {
        this.edibillno = edibillno;
    }

    public List<YYEdiReturnItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiReturnItem> items) {
        this.items = items;
    }


}
