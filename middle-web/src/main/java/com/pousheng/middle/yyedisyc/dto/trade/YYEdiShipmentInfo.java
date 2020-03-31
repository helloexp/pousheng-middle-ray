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
    @JsonProperty(value = "companycode")
    private String companycode;

    /**
     * ERP单号
     */
    @JsonProperty(value = "billno")
    private String billno;

    /**
     * 单据类型
     */
    @JsonProperty(value = "billtype")
    private String billtype;


    /**
     * 上游来源单号，丢件补发或者换货的时候填写的对应的发货单号
     */
    @JsonProperty(value = "sourcebillno")
    private String sourcebillno;

    /**
     * 网店交易单号
     */
    @JsonProperty(value = "shopbillno")
    private String shopbillno;

    /**
     *店铺内码
     */
    @JsonProperty(value = "shopcode")
    private String shopcode;

    /**
     * 店铺名称
     */
    @JsonProperty(value = "shopname")
    private String shopname;

    /**
     * 仓库内码
     */
    @JsonProperty(value = "stockcode")
    private String stockcode;

    /**
     * 出库单类型
     */
    @JsonProperty(value = "refundchangetype")
    private int  refundchangetype;

    /**
     * 发货总的数量
     */
    @JsonProperty(value = "expectqty")
    private int expectqty;

    /**
     *付款时间
     */
    @JsonProperty(value = "paymentdate")
    private String paymentdate;

    /**
     * 客户供应商快递公司内码
     */
    @JsonProperty(value = "customercode")
    private String customercode;

    /**
     * 客户供应商快递公司名称
     */
    @JsonProperty(value = "customername")
    private String customername;

    /**
     * 指定承运商
     */
    @JsonProperty(value = "freightcompany")
    private String freightcompany;

    /**
     *快递方式
     */
    @JsonProperty(value = "expresstype")
    private String expresstype;


    /**
     *快递单号
     */
    @JsonProperty(value = "expressbillno")
    private String expressbillno;

    /**
     *是否开票
     */
    @JsonProperty(value = "isinvoice")
    private int isinvoice;

    /**
     * 是否打印发票
     */
    @JsonProperty(value = "isprintinvoice")
    private int isprintinvoice;

    /**
     * 是否货票同行
     */
    @JsonProperty(value = "istrave")
    private int istrave;

    /**
     * 发票抬头
     */
    @JsonProperty(value = "invoicename")
    private String invoicename;

    /**
     * 发票类型
     */
    @JsonProperty(value = "invoicetype")
    private int invoicetype;

    /**
     * 增值税号
     */
    @JsonProperty(value = "vatnumber")
    private String vatnumber;

    /**
     * 发票内容
     */
    @JsonProperty(value = "invoicecontent")
    private String invoicecontent;
    /**
     * 电子发票邮箱
     */
    @JsonProperty(value = "invoiceemail")
    private String invoiceemail;
    /**
     * 收件人姓名
     */
    @JsonProperty(value = "consigneename")
    private String consigneename;
    /**
     * 收件省
     */
    @JsonProperty(value = "province")
    private String province;
    /**
     * 收件市
     */
    @JsonProperty(value = "city")
    private String city;
    /**
     * 收件区
     */
    @JsonProperty(value = "area")
    private String area;
    /**
     * 地址
     */
    @JsonProperty(value = "address")
    private String address;
    /**
     * 收件邮政编码
     */
    @JsonProperty(value = "zipcode")
    private String zipcode;
    /**
     * 收件人电话
     */
    @JsonProperty(value = "buyertel")
    private String buyertel;
    /**
     * 手机号码
     */
    @JsonProperty(value = "buyermobiletel")
    private String buyermobiletel;
    /**
     * 寄件人姓名
     */
    @JsonProperty(value = "sendcontact")
    private String sendcontact;
    /**
     * 寄件人电话
     */
    @JsonProperty(value = "sendcontacttel")
    private String sendcontacttel;
    /**
     * 寄件省
     */
    @JsonProperty(value = "sendprovince")
    private String sendprovince;
    /**
     * 寄件市
     */
    @JsonProperty(value = "sendcity")
    private String sendcity;
    /**
     * 寄件区
     */
    @JsonProperty(value = "sendarea")
    private String sendarea;
    /**
     * 寄件地址
     */
    @JsonProperty(value = "sendaddress")
    private String sendaddress;
    /**
     * 买家用户名
     */
    @JsonProperty(value = "bcmembername")
    private String bcmembername;
    /**
     * 会员等级
     */
    @JsonProperty(value = "bcmembercard")
    private String bcmembercard;
    /**
     * 支付方式
     */
    @JsonProperty(value = "paymenttype")
    private int paymenttype;
    /**
     * 代收金额
     */
    @JsonProperty(value = "collectionamount")
    private BigDecimal collectionamount;
    /**
     * 结算金额
     */
    @JsonProperty(value = "payamount")
    private BigDecimal payamount;
    /**
     * 买家邮费
     */
    @JsonProperty(value = "expressamount")
    private BigDecimal expressamount;
    /**
     * 线上实付金额
     */
    @JsonProperty(value = "payamountbakup")
    private BigDecimal payamountbakup;
    /**
     * 会员兑换积分
     */
    @JsonProperty(value = "exchangeintegral")
    private BigDecimal exchangeintegral;
    /**
     * 红包支付金额
     */
    @JsonProperty(value = "rptamount")
    private BigDecimal rptamount;
    /**
     * 促销优惠金额
     */
    @JsonProperty(value = "promzramount")
    private BigDecimal promzramount;
    /**
     * 运费到付
     */
    @JsonProperty(value = "freightpay")
    private int freightpay;
    /**
     * 总行数
     */
    @JsonProperty(value = "tdq")
    private int tdq;
    /**
     * ERP最近修改时间
     */
    @JsonProperty(value = "erpmodifytime")
    private String erpmodifytime;

    /**
     * 渠道
     */
    @JsonProperty(value = "channel")
    private String channel;


    /**
     * 下单店铺公司码
     */
    @JsonProperty(value = "shopcompanycode")
    private String shopcompanycode; 
    
    /**
     * ERP单号
     */
    @JsonProperty(value = "erpbillno")
    private String erpbillno;

    /**
     * 2019.04.16 RAY: 訂單來源
     */
    @JsonProperty(value = "billsource")
    private String billsource;   
    
    /**
     * 中台接单时间
     */
    @JsonProperty(value = "billdate")
    private String billdate;
    
    /**
     * 平台下单时间（买家下单时间） 
     */
    @JsonProperty(value = "orderdate")
    private String orderdate;
    
    @JsonIgnore
    public String getBilldate() {
        return billdate;
    }
    
    @JsonIgnore
    public void setBilldate(String billdate) {
        this.billdate = billdate;
    }
    
    @JsonIgnore
    public String getOrderdate() {
        return orderdate;
    }
    
    @JsonIgnore
    public void setOrderdate(String orderdate) {
        this.orderdate = orderdate;
    }

    @JsonIgnore
    public String getBillsource() {
		return billsource;
	}
    @JsonIgnore
	public void setBillsource(String billsource) {
		this.billsource = billsource;
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
    public String getBilltype() {
        return billtype;
    }
    @JsonIgnore
    public void setBilltype(String billtype) {
        this.billtype = billtype;
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
    public String getStockcode() {
        return stockcode;
    }
    @JsonIgnore
    public void setStockcode(String stockcode) {
        this.stockcode = stockcode;
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
    public int getExpectqty() {
        return expectqty;
    }
    @JsonIgnore
    public void setExpectqty(int expectqty) {
        this.expectqty = expectqty;
    }
    @JsonIgnore
    public String getPaymentdate() {
        return paymentdate;
    }
    @JsonIgnore
    public void setPaymentdate(String paymentdate) {
        this.paymentdate = paymentdate;
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
    public String getFreightcompany() {
        return freightcompany;
    }
    @JsonIgnore
    public void setFreightcompany(String freightcompany) {
        this.freightcompany = freightcompany;
    }
    @JsonIgnore
    public String getExpresstype() {
        return expresstype;
    }
    @JsonIgnore
    public void setExpresstype(String expresstype) {
        this.expresstype = expresstype;
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
    public int getIsinvoice() {
        return isinvoice;
    }
    @JsonIgnore
    public void setIsinvoice(int isinvoice) {
        this.isinvoice = isinvoice;
    }
    @JsonIgnore
    public int getIsprintinvoice() {
        return isprintinvoice;
    }
    @JsonIgnore
    public void setIsprintinvoice(int isprintinvoice) {
        this.isprintinvoice = isprintinvoice;
    }
    @JsonIgnore
    public String getInvoicename() {
        return invoicename;
    }
    @JsonIgnore
    public void setInvoicename(String invoicename) {
        this.invoicename = invoicename;
    }
    @JsonIgnore
    public int getInvoicetype() {
        return invoicetype;
    }
    @JsonIgnore
    public void setInvoicetype(int invoicetype) {
        this.invoicetype = invoicetype;
    }
    @JsonIgnore
    public String getVatnumber() {
        return vatnumber;
    }
    @JsonIgnore
    public void setVatnumber(String vatnumber) {
        this.vatnumber = vatnumber;
    }
    @JsonIgnore
    public String getInvoicecontent() {
        return invoicecontent;
    }
    @JsonIgnore
    public void setInvoicecontent(String invoicecontent) {
        this.invoicecontent = invoicecontent;
    }
    @JsonIgnore
    public String getInvoiceemail() {
        return invoiceemail;
    }
    @JsonIgnore
    public void setInvoiceemail(String invoiceemail) {
        this.invoiceemail = invoiceemail;
    }
    @JsonIgnore
    public String getConsigneename() {
        return consigneename;
    }
    @JsonIgnore
    public void setConsigneename(String consigneename) {
        this.consigneename = consigneename;
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
    public String getBuyertel() {
        return buyertel;
    }
    @JsonIgnore
    public void setBuyertel(String buyertel) {
        this.buyertel = buyertel;
    }
    @JsonIgnore
    public String getBuyermobiletel() {
        return buyermobiletel;
    }
    @JsonIgnore
    public void setBuyermobiletel(String buyermobiletel) {
        this.buyermobiletel = buyermobiletel;
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
    public String getBcmembername() {
        return bcmembername;
    }
    @JsonIgnore
    public void setBcmembername(String bcmembername) {
        this.bcmembername = bcmembername;
    }
    @JsonIgnore
    public String getBcmembercard() {
        return bcmembercard;
    }
    @JsonIgnore
    public void setBcmembercard(String bcmembercard) {
        this.bcmembercard = bcmembercard;
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
    public BigDecimal getPayamount() {
        return payamount;
    }
    @JsonIgnore
    public void setPayamount(BigDecimal payamount) {
        this.payamount = payamount;
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
    public BigDecimal getPayamountbakup() {
        return payamountbakup;
    }
    @JsonIgnore
    public void setPayamountbakup(BigDecimal payamountbakup) {
        this.payamountbakup = payamountbakup;
    }
    @JsonIgnore
    public BigDecimal getExchangeintegral() {
        return exchangeintegral;
    }
    @JsonIgnore
    public void setExchangeintegral(BigDecimal exchangeintegral) {
        this.exchangeintegral = exchangeintegral;
    }
    @JsonIgnore
    public BigDecimal getRptamount() {
        return rptamount;
    }
    @JsonIgnore
    public void setRptamount(BigDecimal rptamount) {
        this.rptamount = rptamount;
    }
    @JsonIgnore
    public BigDecimal getPromzramount() {
        return promzramount;
    }
    @JsonIgnore
    public void setPromzramount(BigDecimal promzramount) {
        this.promzramount = promzramount;
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
    public String getErpmodifytime() {
        return erpmodifytime;
    }
    @JsonIgnore
    public void setErpmodifytime(String erpmodifytime) {
        this.erpmodifytime = erpmodifytime;
    }
    @JsonIgnore
    public String getShopcompanycode() {
        return shopcompanycode;
    }
    @JsonIgnore
    public void setShopcompanycode(String shopcompanycode) {
        this.shopcompanycode = shopcompanycode;
    }
    @JsonIgnore
    public String getErpbillno() {
        return erpbillno;
    }
    @JsonIgnore
    public void setErpbillno(String erpbillno) {
        this.erpbillno = erpbillno;
    }
    @JsonIgnore
    public int getIstrave() {
        return istrave;
    }
    @JsonIgnore
    public void setIstrave(int istrave) {
        this.istrave = istrave;
    }
    @JsonIgnore
    public String getProvince() {
        return province;
    }
    @JsonIgnore
    public void setProvince(String province) {
        this.province = province;
    }
    @JsonIgnore
    public String getCity() {
        return city;
    }
    @JsonIgnore
    public void setCity(String city) {
        this.city = city;
    }
    @JsonIgnore
    public String getArea() {
        return area;
    }
    @JsonIgnore
    public void setArea(String area) {
        this.area = area;
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
    public int getPaymenttype() {
        return paymenttype;
    }
    @JsonIgnore
    public void setPaymenttype(int paymenttype) {
        this.paymenttype = paymenttype;
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
    public String getChannel() {
        return channel;
    }
    @JsonIgnore
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * 明细

     */
    private List<YYEdiShipmentItem> items;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }


    public List<YYEdiShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<YYEdiShipmentItem> items) {
        this.items = items;
    }
}
