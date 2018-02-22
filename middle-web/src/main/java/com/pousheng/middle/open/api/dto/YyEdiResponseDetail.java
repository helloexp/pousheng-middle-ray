package com.pousheng.middle.open.api.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/23
 * pousheng-middle
 */
@Data
public class YyEdiResponseDetail implements java.io.Serializable{
    private static final long serialVersionUID = 5145034654292054894L;
    /**
     * 中台发货单号
     */
    private Long shipmentId;
    /**
     * yyedi发货单号
     */
    private String yyEdiShipmentId;

    /**
     * 200代表成功，-100代表失败
     */
    private String errorCode;
    /**
     * 错误信息
     */
    private String errorMsg;
}
