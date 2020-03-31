package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Data
public class ESPExpressCodeCancelRequestContent {
    //公司代码
    private String companycode;
    //OXO订单号
    private String billno;
    private String channelcode;
    private String remark;
}
