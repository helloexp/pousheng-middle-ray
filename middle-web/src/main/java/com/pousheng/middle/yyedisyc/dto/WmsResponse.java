package com.pousheng.middle.yyedisyc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 同步WMS发货单响应
 *
 * @author tanlongjun
 */
@Data
public class WmsResponse implements Serializable {

    private static final long serialVersionUID = -1670470842519862658L;
    /**
     * 返回代码
     * "00000":请求成功返回
     * "00000":该订单已存在
     * "10000":非法报文、报文解析异常
     * "10010":业务数据异常
     * "10020":服务SID不存在
     * "10030":应用编码不可用
     * "99999":未处理的Runtime系统级异常
     */
    private String code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 业务出现的详细异常信息或者数据
     */
    private String returnJson;

}
