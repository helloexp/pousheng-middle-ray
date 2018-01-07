package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String CollectionAmount;
    /**
     * 结算金额
     */
    @JsonProperty(value = "PayAmount")
    private String PayAmount;
    /**
     * 买家邮费
     */
    @JsonProperty(value = "ExpressAmount")
    private String ExpressAmount;
    /**
     * 线上实付金额
     */
    @JsonProperty(value = "PayAmountBakUp")
    private String PayAmountBakUp;
    /**
     * 会员兑换积分
     */
    @JsonProperty(value = "ExchangeIntegral")
    private String ExchangeIntegral;
    /**
     * 红包支付金额
     */
    @JsonProperty(value = "RptAmount")
    private String RptAmount;
    /**
     * 促销优惠金额
     */
    @JsonProperty(value = "PromZRAmount")
    private String PromZRAmount;
    /**
     * 运费到付
     */
    @JsonProperty(value = "FreightPay")
    private String FreightPay;
    /**
     * 总行数
     */
    @JsonProperty(value = "Tdq")
    private String Tdq;
    /**
     * ERP最近修改时间
     */
    @JsonProperty(value = "ERPModifyTime")
    private String ERPModifyTime;

    /**
     * 明细
     */
    private List<YYEdiShipmentItem> items;

}
