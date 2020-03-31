package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/9/4
 */
@Data
public class StoreShipmentShipedEvent {
    //OXO订单号
    private String billno;
    //中通快递	ZTO
    //顺丰快递	SF
    //圆通快递	YTO
    private String expresscode;
    //运单号
    private String expressbillno;
    //发货单号
    private Long shipmentId;
}
