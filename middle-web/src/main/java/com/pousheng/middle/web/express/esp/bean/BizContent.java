package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

import java.util.List;

@Data
public class BizContent {
    //公司内码
    private String companycode;
    //中台订单号
    private String billno;
    //订单类型，SalesBC 电商销售单，Offline  线下门店,
    private String billtype;
    //非必填，来源单号，补发，换发时填写上次上游发货单号
    private String sourcebillno;
    //非必填，网店交易单号，非必填
    private String shopbillno;
    //店铺外码
    private String shopcode;
    //店铺名称
    private String shopname;
    //出库单类型
    //1 正常销售单
    //2 换发
    //3 补发
    private String refundchangetype;
    //非必填，发货仓公司内码
    private String stockcompanycode;
    //发货仓别代码
    private String stockcode;
    //预计数量
    private String expectqty;
    //付款时间
    private String paymentdate;
    //非必填，客户供应商快递公司内码如：YTO、STO
    private String customercode;
    //非必填，客户供应商快递公司名称
    private String customername;
    //发货类型，10 店发，20 仓发
    private String deliverytype;
    //快递公司的月结账号
    private String expressaccountid;
    //非必填，指定承运商
    private String freightcompany;
    //非必填，运单号，当渠道channel是vipoxo时必填
    private String expressbillno;
    //快递方式
    //Express=走快递，
    //ZT =自提,
    //‘’=其它
    private String expresstype;
    //非必填，是否开票，1=是，0=否
    private String isinvoice;
    //非必填，是否打印发票，1=是，0=否　默认 0
    private String isprintinvoice;
    //非必填，是否货票同行
    private String istrave;
    //非必填，发票抬头
    private String invoicename;
    //非必填，增值税号
    private String vatnumber;
    //非必填，发票内容
    private String invoicecontent;
    //收件人姓名
    private String consigneename;
    //收件省
    private String province;
    //市
    private String city;
    //区
    private String area;
    //地址
    private String address;
    //非必填，邮编
    private String zipcode;
    //非必填，收件人电话
    private String buyertel;
    //手机号码
    private String buyermobiletel;
    //非必填，寄件人姓名
    private String sendcontact;
    //非必填，寄件人电话
    private String sendcontacttel;
    //非必填，
    private String sendprovince;
    //非必填，
    private String sendcity;
    //非必填，
    private String sendarea;
    //非必填，
    private String sendaddress;
    //买家用户名
    private String bcmembername;
    //非必填，会员等级
    private String bcmembercard;
    //支付方式
    // 1  货到付款
    //0  在线付款
    private String paymenttype;
    //代收金额
    // COD = PayAmount，
    //其它=　0
    private String collectionamount;
    //结算金额(实付金额)
    private String payamount;
    //买家邮费
    private String expressamount;
    //线上实付金额=payamount
    private String payamountbakup;
    //非必填，会员兑换积分
    private String exchangeintegral;
    //非必填，红包支付金额
    private String rptamount;
    //非必填，促销优惠金额
    private String promzramount;
    //非必填，运费到付
    private String freightpay;
    //是否通知快递员上门取件
    //1：要求上门取件，
    //0：不要求
    private String iscall;
    //总行数
    private String tdq;
    //
    private List<Items> items;
    //非必填，店发时必填
    private String deliveryshopcode;
    //发货仓公司代码
    private String deliverycompanycode;
    //包裹体积
    private double volume;
    //订单渠道
    //MPOS订单：mpos
    //OXO订单：vipoxo
    private String channel;
    //下单时间
    private String orderdatetime;
    //发货时间
    private String deliverydatetime;
    //重量
    private double weight;
    //获取快递单号1, 回传快递单号0
    private String isgetexpress;

}