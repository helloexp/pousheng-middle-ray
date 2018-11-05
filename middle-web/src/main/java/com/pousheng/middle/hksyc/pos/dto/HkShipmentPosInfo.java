package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2018/1/16
 */
@Data
public class HkShipmentPosInfo implements Serializable{

    private static final long serialVersionUID = 1250068655320923604L;

    private String     manualbillno; //第三方平台单号
    private String     buyeralipayno; //支付宝账号
    private String     alipaybillno; //支付交易号
    private String     sourceremark; //订单来源说明
    private String     ordertype;  //订单类型
    private String     ordercustomercode; //订单标记code
    private String     appamtsourceshop; //业绩来源店铺
    private String     paymentdate; //付款时间
    private String     cardcode ; //会员卡号
    private String     buyercode; //买家昵称
    private String     buyermobiletel; //买家手机
    private String     buyertel; //买家座机
    private String     buyerremark; //买家留言
    private String     consigneename;  //收货人姓名
    private String     payamountbakup; //线上实付金额
    private String     zramount; //优惠金额
    private String     expresscost; //邮费成本
    private String     codcharges; //货到付款服务费
    private String     preremark; //优惠信息
    private String    isinvoice; //是否开票
    private String    invoice_name; //发票抬头
    private String    taxno; //税号
    private String    province; //省
    private String    city; //市
    private String    area; //区
    private String    zipcode; //邮政编码
    private String    address; //详细地址
    private String    sellremark; //卖家备注
    private String    sellcode; //卖家昵称
    private String    expresstype; //物流方式
    private String    vendcustcode; //物流公司代码
    private String    expressbillno; //物流单号
    private String    wms_ordercode; //第三方物流单号
    private String    consignmentdate; //发货时间
    private String    weight;//重量
    private String    parcelweight;//包裹重量
    private String    dischargeintegral;//会员抵现积分
    private String    dischargeamount;//会员抵现金额
}
