package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ESPExpressResponse {
    //返回码
    //"00000":请求成功返回
    //"00000":该订单已存在
    //"10000":非法报文、报文解析异常
    //"10010":业务数据异常
    //"10020":服务SID不存在
    //"10030":应用编码不可用
    //"99999":未处理的Runtime系统级异常
    private String code;
    private String message;
    private List<ESPExpressResponseContent> returnJson;

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setReturnJson(List<ESPExpressResponseContent> returnJson) {
        this.returnJson = returnJson;
    }

    public List<ESPExpressResponseContent> getReturnJson() {
        return returnJson;
    }

}