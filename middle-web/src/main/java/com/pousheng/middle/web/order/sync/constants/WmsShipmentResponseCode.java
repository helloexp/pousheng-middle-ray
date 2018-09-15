package com.pousheng.middle.web.order.sync.constants;

import lombok.Getter;

/**
 * WMS发货单响应码
 * @author tanlongjun
 */
public enum WmsShipmentResponseCode {

    SUCCESS("00000", "请求成功返回/该订单已存在"),
    ILLEGAL("10000", "非法报文、报文解析异常"),
    BIZ_DATA_EXCEPTION("10010", "业务数据异常"),
    SID_NOT_EXIST("10020", "服务SID不存在"),
    APP_CODE_Unavailable("10030", "应用编码不可用"),
    UNHANDLE_EXCEPTION("99999", "未处理的Runtime系统级异常");

    @Getter
    private String code;

    @Getter
    private String desc;

    private WmsShipmentResponseCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
