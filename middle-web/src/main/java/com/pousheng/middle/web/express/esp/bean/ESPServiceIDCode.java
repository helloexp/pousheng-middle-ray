package com.pousheng.middle.web.express.esp.bean;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
public class ESPServiceIDCode {
    //获取快递单号接口sid
    public static final String getExpressNo = "PS_ERP_TP_getexpressno";
    //仓发接收OXO运单号接口
    public static final String sendOXOExpressNo = "PS_ERP_WMS_oxoexpressno";
    //
    public static final String sendWarehouseExpressNo = "PS_ERP_WMS_oxoexpressno";

    public static final String cancelOXOExpressNo = "PS_ERP_TP_cancelorder";
}
