package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Data
public class ESPExpressCodeSendRequestContent {
    //公司代码
    private String companycode;
    //OXO订单号
    private String billno;
    //中通快递	ZTO
    //顺丰快递	SF
    //圆通快递	YTO
    private String expresscode;
    //运单号
    private String expressbillno;
    //是否拆单，不拆0
    private String issplitbill;
}
