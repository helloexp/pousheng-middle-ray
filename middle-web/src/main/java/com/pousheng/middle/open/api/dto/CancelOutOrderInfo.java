package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 取消订单
 * Created by songrenfei on 2018/4/1
 */
@Data
public class CancelOutOrderInfo implements Serializable{


    private static final long serialVersionUID = -6971791233605878247L;

    private String outOrderId; //订单id
    private String outSkuOrderId;//子订单id 如果该字段不传默认整个订单取消
    private String channel; //渠道 云聚默认填:YJ
    private String buyerNote;//买家备注
    private String sellerNote;//商家备注
    private String applyAt; //申请时间 yyyyMMdd HHmmss


}
