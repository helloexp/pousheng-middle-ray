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
public class YYEdiShipmentInfo implements java.io.Serializable {
    private static final long serialVersionUID = -1859083644408018761L;

    /**
     * 公司码
     */
    @JsonProperty(value = "CompanyCode")
    private String CompanyCode;

    /**
     * ERP单号
     */
    @JsonProperty(value = "BillNo")
    private String BillNo;

    /**
     * 单据类型
     */
    @JsonProperty(value = "BillType")
    private String BillType;


    /**
     * 上游来源单号，丢件补发或者换货的时候填写的对应的发货单号
     */
    @JsonProperty(value = "SourceBillNo")
    private String SourceBillNo;

    /**
     * 网店交易单号
     */
    @JsonProperty(value = "ShopBillNo")
    private String ShopBillNo;

    /**
     *店铺内码
     */
    @JsonProperty(value = "ShopCode")
    private String ShopCode;

    /**
     * 店铺名称
     */
    @JsonProperty(value = "ShopName")
    private String ShopName;

    /**
     * 仓库内码
     */
    @JsonProperty(value = "StockCode")
    private String StockCode;

    /**
     * 出库单类型
     */
    @JsonProperty(value = "RefundChangeType")
    private int  RefundChangeType;

    /**
     * 发货总的数量
     */
    @JsonProperty(value = "ExpectQty")
    private int ExpectQty;

    /**
     *付款时间
     */
    @JsonProperty(value = "PaymentDate")
    private String PaymentDate;

    /**
     * 客户供应商快递公司内码
     */
    @JsonProperty(value = "CustomerCode")
    private String CustomerCode;

    /**
     * 客户供应商快递公司名称
     */
    @JsonProperty(value = "CustomerName")
    private String CustomerName;

    /**
     * 指定承运商
     */
    @JsonProperty(value = "FreightCompany")
    private String FreightCompany;

    /**
     *快递方式
     */
    @JsonProperty(value = "ExpressType")
    private String ExpressType;


    /**
     *快递单号
     */
    @JsonProperty(value = "ExpressBillNo")
    private String ExpressBillNo;

    /**
     *是否开票
     */
    @JsonProperty(value = "IsInvoice")
    private int IsInvoice;

    /**
     * 是否打印发票
     */
    @JsonProperty(value = "IsPrintInvoice")
    private int IsPrintInvoice;

    /**
     * 是否货票同行
     */
    @JsonProperty(value = "Istrave")
    private int Istrave;

    /**
     * 发票抬头
     */
    @JsonProperty(value = "InvoiceName")
    private String InvoiceName;

    /**
     * 发票类型
     */
    @JsonProperty(value = "InvoiceType")
    private int InvoiceType;

    /**
     * 增值税号
     */
    @JsonProperty(value = "VATNumber")
    private String VATNumber;

    /**
     * 发票内容
     */
    @JsonProperty(value = "InvoiceContent")
    private String InvoiceContent;
    /**
     * 电子发票邮箱
     */
    @JsonProperty(value = "InvoiceEmail")
    private String InvoiceEmail;
    /**
     * 收件人姓名
     */
    @JsonProperty(value = "ConsigneeName")
    private String ConsigneeName;
    /**
     * 收件省
     */
    @JsonProperty(value = "Province")
    private String Province;
    /**
     * 收件市
     */
    @JsonProperty(value = "City")
    private String City;
    /**
     * 收件区
     */
    @JsonProperty(value = "Area")
    private String Area;
    /**
     * 地址
     */
    @JsonProperty(value = "Address")
    private String Address;
    /**
     * 收件邮政编码
     */
    @JsonProperty(value = "ZipCode")
    private String ZipCode;
    /**
     * 收件人电话
     */
    @JsonProperty(value = "BuyerTel")
    private String BuyerTel;
    /**
     * 手机号码
     */
    @JsonProperty(value = "BuyerMobileTel")
    private String BuyerMobileTel;
    /**
     * 寄件人姓名
     */
    @JsonProperty(value = "SendContact")
    private String SendContact;
    /**
     * 寄件人电话
     */
    @JsonProperty(value = "SendContactTel")
    private String SendContactTel;
    /**
     * 寄件省
     */
    @JsonProperty(value = "SendProvince")
    private String SendProvince;
    /**
     * 寄件市
     */
    @JsonProperty(value = "SendCity")
    private String SendCity;
    /**
     * 寄件区
     */
    @JsonProperty(value = "SendArea")
    private String SendArea;
    /**
     * 寄件地址
     */
    @JsonProperty(value = "SendAddress")
    private String SendAddress;
    /**
     * 买家用户名
     */
    @JsonProperty(value = "BCMemberName")
    private String BCMemberName;
    /**
     * 会员等级
     */
    @JsonProperty(value = "BCMemberCard")
    private String BCMemberCard;
    /**
     * 支付方式
     */
    @JsonProperty(value = "Paymenttype")
    private int Paymenttype;
    /**
     * 代收金额
     */
    @JsonProperty(value = "CollectionAmount")
    private BigDecimal CollectionAmount;
    /**
     * 结算金额
     */
    @JsonProperty(value = "PayAmount")
    private BigDecimal PayAmount;
    /**
     * 买家邮费
     */
    @JsonProperty(value = "ExpressAmount")
    private BigDecimal ExpressAmount;
    /**
     * 线上实付金额
     */
    @JsonProperty(value = "PayAmountBakUp")
    private BigDecimal PayAmountBakUp;
    /**
     * 会员兑换积分
     */
    @JsonProperty(value = "ExchangeIntegral")
    private BigDecimal ExchangeIntegral;
    /**
     * 红包支付金额
     */
    @JsonProperty(value = "RptAmount")
    private BigDecimal RptAmount;
    /**
     * 促销优惠金额
     */
    @JsonProperty(value = "PromZRAmount")
    private BigDecimal PromZRAmount;
    /**
     * 运费到付
     */
    @JsonProperty(value = "FreightPay")
    private int FreightPay;
    /**
     * 总行数
     */
    @JsonProperty(value = "Tdq")
    private int Tdq;
    /**
     * ERP最近修改时间
     */
    @JsonProperty(value = "ERPModifyTime")
    private String ERPModifyTime;

    /**
     * 渠道
     */
    @JsonProperty(value = "Channel")
    private String Channel;


    /**
     * 下单店铺公司码
     */
    @JsonProperty(value = "ShopCompanyCode")
    private String ShopCompanyCode;


    /**
     * ERP单号
     */
    @JsonProperty(value = "ERPBillNo")
    private String ERPBillNo;


    /**
     * 明细
     */
    private List<YYEdiShipmentItem> items;

    public static long getSerialVersionUID() {
        return serialVersionUID;
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
    public String getBillType() {
        return BillType;
    }
    @JsonIgnore
    public void setBillType(String billType) {
        BillType = billType;
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
    public String getStockCode() {
        return StockCode;
    }
    @JsonIgnore
    public void setStockCode(String stockCode) {
        StockCode = stockCode;
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
    public int getExpectQty() {
        return ExpectQty;
    }
    @JsonIgnore
    public void setExpectQty(int expectQty) {
        ExpectQty = expectQty;
    }
    @JsonIgnore
    public String getPaymentDate() {
        return PaymentDate;
    }
    @JsonIgnore
    public void setPaymentDate(String paymentDate) {
        PaymentDate = paymentDate;
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
    public String getFreightCompany() {
        return FreightCompany;
    }
    @JsonIgnore
    public void setFreightCompany(String freightCompany) {
        FreightCompany = freightCompany;
    }
    @JsonIgnore
    public String getExpressType() {
        return ExpressType;
    }
    @JsonIgnore
    public void setExpressType(String expressType) {
        ExpressType = expressType;
    }
    @JsonIgnore
    public int getIsInvoice() {
        return IsInvoice;
    }
    @JsonIgnore
    public void setIsInvoice(int isInvoice) {
        IsInvoice = isInvoice;
    }
    @JsonIgnore
    public int getIsPrintInvoice() {
        return IsPrintInvoice;
    }
    @JsonIgnore
    public void setIsPrintInvoice(int isPrintInvoice) {
        IsPrintInvoice = isPrintInvoice;
    }
    @JsonIgnore
    public int getIstrave() {
        return Istrave;
    }
    @JsonIgnore
    public void setIstrave(int istrave) {
        Istrave = istrave;
    }
    @JsonIgnore
    public String getInvoiceName() {
        return InvoiceName;
    }
    @JsonIgnore
    public void setInvoiceName(String invoiceName) {
        InvoiceName = invoiceName;
    }
    @JsonIgnore
    public int getInvoiceType() {
        return InvoiceType;
    }
    @JsonIgnore
    public void setInvoiceType(int invoiceType) {
        InvoiceType = invoiceType;
    }
    @JsonIgnore
    public String getVATNumber() {
        return VATNumber;
    }
    @JsonIgnore
    public void setVATNumber(String VATNumber) {
        this.VATNumber = VATNumber;
    }
    @JsonIgnore
    public String getInvoiceContent() {
        return InvoiceContent;
    }
    @JsonIgnore
    public void setInvoiceContent(String invoiceContent) {
        InvoiceContent = invoiceContent;
    }
    @JsonIgnore
    public String getInvoiceEmail() {
        return InvoiceEmail;
    }
    @JsonIgnore
    public void setInvoiceEmail(String invoiceEmail) {
        InvoiceEmail = invoiceEmail;
    }
    @JsonIgnore
    public String getConsigneeName() {
        return ConsigneeName;
    }
    @JsonIgnore
    public void setConsigneeName(String consigneeName) {
        ConsigneeName = consigneeName;
    }
    @JsonIgnore
    public String getProvince() {
        return Province;
    }
    @JsonIgnore
    public void setProvince(String province) {
        Province = province;
    }
    @JsonIgnore
    public String getCity() {
        return City;
    }
    @JsonIgnore
    public void setCity(String city) {
        City = city;
    }
    @JsonIgnore
    public String getArea() {
        return Area;
    }
    @JsonIgnore
    public void setArea(String area) {
        Area = area;
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
    public String getZipCode() {
        return ZipCode;
    }
    @JsonIgnore
    public void setZipCode(String zipCode) {
        ZipCode = zipCode;
    }
    @JsonIgnore
    public String getBuyerTel() {
        return BuyerTel;
    }
    @JsonIgnore
    public void setBuyerTel(String buyerTel) {
        BuyerTel = buyerTel;
    }
    @JsonIgnore
    public String getBuyerMobileTel() {
        return BuyerMobileTel;
    }
    @JsonIgnore
    public void setBuyerMobileTel(String buyerMobileTel) {
        BuyerMobileTel = buyerMobileTel;
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
    public String getBCMemberName() {
        return BCMemberName;
    }
    @JsonIgnore
    public void setBCMemberName(String BCMemberName) {
        this.BCMemberName = BCMemberName;
    }
    @JsonIgnore
    public String getBCMemberCard() {
        return BCMemberCard;
    }
    @JsonIgnore
    public void setBCMemberCard(String BCMemberCard) {
        this.BCMemberCard = BCMemberCard;
    }
    @JsonIgnore
    public int getPaymenttype() {
        return Paymenttype;
    }
    @JsonIgnore
    public void setPaymenttype(int paymenttype) {
        Paymenttype = paymenttype;
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
    public BigDecimal getPayAmount() {
        return PayAmount;
    }
    @JsonIgnore
    public void setPayAmount(BigDecimal payAmount) {
        PayAmount = payAmount;
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
    public BigDecimal getPayAmountBakUp() {
        return PayAmountBakUp;
    }
    @JsonIgnore
    public void setPayAmountBakUp(BigDecimal payAmountBakUp) {
        PayAmountBakUp = payAmountBakUp;
    }
    @JsonIgnore
    public BigDecimal getExchangeIntegral() {
        return ExchangeIntegral;
    }
    @JsonIgnore
    public void setExchangeIntegral(BigDecimal exchangeIntegral) {
        ExchangeIntegral = exchangeIntegral;
    }
    @JsonIgnore
    public BigDecimal getRptAmount() {
        return RptAmount;
    }
    @JsonIgnore
    public void setRptAmount(BigDecimal rptAmount) {
        RptAmount = rptAmount;
    }
    @JsonIgnore
    public BigDecimal getPromZRAmount() {
        return PromZRAmount;
    }
    @JsonIgnore
    public void setPromZRAmount(BigDecimal promZRAmount) {
        PromZRAmount = promZRAmount;
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
    @JsonIgnore
    public String getChannel() {
        return Channel;
    }
    @JsonIgnore
    public void setChannel(String channel) {
        Channel = channel;
    }
    @JsonIgnore
    public String getShopCompanyCode() {
        return ShopCompanyCode;
    }
    @JsonIgnore
    public void setShopCompanyCode(String shopCompanyCode) {
        ShopCompanyCode = shopCompanyCode;
    }
    @JsonIgnore
    public String getERPBillNo() {
        return ERPBillNo;
    }
    @JsonIgnore
    public void setERPBillNo(String ERPBillNo) {
        this.ERPBillNo = ERPBillNo;
    }
    @JsonIgnore
    public String getExpressBillNo() {
        return ExpressBillNo;
    }
    @JsonIgnore
    public void setExpressBillNo(String expressBillNo) {
        ExpressBillNo = expressBillNo;
    }

    public List<YYEdiShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiShipmentItem> items) {
        this.items = items;
    }
}
