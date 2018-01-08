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
    private Date PaymentDate;

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
    private String PayAmount;
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
     * 明细
     */
    private List<YYEdiShipmentItem> items;

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public String getBillType() {
        return BillType;
    }

    public void setBillType(String billType) {
        BillType = billType;
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

    public String getStockCode() {
        return StockCode;
    }

    public void setStockCode(String stockCode) {
        StockCode = stockCode;
    }

    public int getRefundChangeType() {
        return RefundChangeType;
    }

    public void setRefundChangeType(int refundChangeType) {
        RefundChangeType = refundChangeType;
    }

    public int getExpectQty() {
        return ExpectQty;
    }

    public void setExpectQty(int expectQty) {
        ExpectQty = expectQty;
    }

    public Date getPaymentDate() {
        return PaymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        PaymentDate = paymentDate;
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

    public String getFreightCompany() {
        return FreightCompany;
    }

    public void setFreightCompany(String freightCompany) {
        FreightCompany = freightCompany;
    }

    public String getExpressType() {
        return ExpressType;
    }

    public void setExpressType(String expressType) {
        ExpressType = expressType;
    }

    public int getIsInvoice() {
        return IsInvoice;
    }

    public void setIsInvoice(int isInvoice) {
        IsInvoice = isInvoice;
    }

    public int getIsPrintInvoice() {
        return IsPrintInvoice;
    }

    public void setIsPrintInvoice(int isPrintInvoice) {
        IsPrintInvoice = isPrintInvoice;
    }

    public int getIstrave() {
        return Istrave;
    }

    public void setIstrave(int istrave) {
        Istrave = istrave;
    }

    public String getInvoiceName() {
        return InvoiceName;
    }

    public void setInvoiceName(String invoiceName) {
        InvoiceName = invoiceName;
    }

    public int getInvoiceType() {
        return InvoiceType;
    }

    public void setInvoiceType(int invoiceType) {
        InvoiceType = invoiceType;
    }

    public String getVATNumber() {
        return VATNumber;
    }

    public void setVATNumber(String VATNumber) {
        this.VATNumber = VATNumber;
    }

    public String getInvoiceContent() {
        return InvoiceContent;
    }

    public void setInvoiceContent(String invoiceContent) {
        InvoiceContent = invoiceContent;
    }

    public String getInvoiceEmail() {
        return InvoiceEmail;
    }

    public void setInvoiceEmail(String invoiceEmail) {
        InvoiceEmail = invoiceEmail;
    }

    public String getConsigneeName() {
        return ConsigneeName;
    }

    public void setConsigneeName(String consigneeName) {
        ConsigneeName = consigneeName;
    }

    public String getProvince() {
        return Province;
    }

    public void setProvince(String province) {
        Province = province;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getArea() {
        return Area;
    }

    public void setArea(String area) {
        Area = area;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public String getZipCode() {
        return ZipCode;
    }

    public void setZipCode(String zipCode) {
        ZipCode = zipCode;
    }

    public String getBuyerTel() {
        return BuyerTel;
    }

    public void setBuyerTel(String buyerTel) {
        BuyerTel = buyerTel;
    }

    public String getBuyerMobileTel() {
        return BuyerMobileTel;
    }

    public void setBuyerMobileTel(String buyerMobileTel) {
        BuyerMobileTel = buyerMobileTel;
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

    public String getBCMemberName() {
        return BCMemberName;
    }

    public void setBCMemberName(String BCMemberName) {
        this.BCMemberName = BCMemberName;
    }

    public String getBCMemberCard() {
        return BCMemberCard;
    }

    public void setBCMemberCard(String BCMemberCard) {
        this.BCMemberCard = BCMemberCard;
    }

    public int getPaymenttype() {
        return Paymenttype;
    }

    public void setPaymenttype(int paymenttype) {
        Paymenttype = paymenttype;
    }

    public BigDecimal getCollectionAmount() {
        return CollectionAmount;
    }

    public void setCollectionAmount(BigDecimal collectionAmount) {
        CollectionAmount = collectionAmount;
    }

    public String getPayAmount() {
        return PayAmount;
    }

    public void setPayAmount(String payAmount) {
        PayAmount = payAmount;
    }

    public BigDecimal getExpressAmount() {
        return ExpressAmount;
    }

    public void setExpressAmount(BigDecimal expressAmount) {
        ExpressAmount = expressAmount;
    }

    public BigDecimal getPayAmountBakUp() {
        return PayAmountBakUp;
    }

    public void setPayAmountBakUp(BigDecimal payAmountBakUp) {
        PayAmountBakUp = payAmountBakUp;
    }

    public BigDecimal getExchangeIntegral() {
        return ExchangeIntegral;
    }

    public void setExchangeIntegral(BigDecimal exchangeIntegral) {
        ExchangeIntegral = exchangeIntegral;
    }

    public BigDecimal getRptAmount() {
        return RptAmount;
    }

    public void setRptAmount(BigDecimal rptAmount) {
        RptAmount = rptAmount;
    }

    public BigDecimal getPromZRAmount() {
        return PromZRAmount;
    }

    public void setPromZRAmount(BigDecimal promZRAmount) {
        PromZRAmount = promZRAmount;
    }

    public int getFreightPay() {
        return FreightPay;
    }

    public void setFreightPay(int freightPay) {
        FreightPay = freightPay;
    }

    public int getTdq() {
        return Tdq;
    }

    public void setTdq(int tdq) {
        Tdq = tdq;
    }

    public String getERPModifyTime() {
        return ERPModifyTime;
    }

    public void setERPModifyTime(String ERPModifyTime) {
        this.ERPModifyTime = ERPModifyTime;
    }

    public List<YYEdiShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiShipmentItem> items) {
        this.items = items;
    }
}
