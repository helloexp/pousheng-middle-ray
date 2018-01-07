package com.pousheng.middle.yyedisyc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Description: add something here
 * User: xiao
 * Date: 03/07/2017
 */
@Data
public class YYEdiRequestHead implements Serializable {
    private static final long serialVersionUID = 2006897661900711291L;
    private String appCode;
    private String format = "json";
    private String isSign = "0";
    private String method;                  // 接口方法名
    private String sendTime;                // yyyy-MM-dd HH:mm:ss
    private String signBody = "";
    private String version = "2.0";
    private String serialNo;


    public static YYEdiRequestHead create() {
        return new YYEdiRequestHead();
    }

    public YYEdiRequestHead appCode(String appCode) {
        this.appCode = appCode;
        return this;
    }

    public YYEdiRequestHead format(String format) {
        this.format = format;
        return this;
    }

    public YYEdiRequestHead isSign(String isSign) {
        this.isSign = isSign;
        return this;
    }

    public YYEdiRequestHead method(String method) {
        this.method = method;
        return this;
    }

    public YYEdiRequestHead sendTime(String sendTime) {
        this.sendTime = sendTime;
        return this;
    }

    public YYEdiRequestHead signBody(String signBody) {
        this.signBody = signBody;
        return this;
    }

    public YYEdiRequestHead version(String version) {
        this.version = version;
        return this;
    }

    public YYEdiRequestHead serialNo(String serialNo) {
        this.serialNo = serialNo;
        return this;
    }




}
