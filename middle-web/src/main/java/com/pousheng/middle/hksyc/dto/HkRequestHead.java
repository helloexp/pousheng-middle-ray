package com.pousheng.middle.hksyc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Description: add something here
 * User: xiao
 * Date: 03/07/2017
 */
@Data
public class HkRequestHead implements Serializable {
    private static final long serialVersionUID = 2006897661900711291L;
    private String appCode;
    private String format = "json";
    private String isSign = "0";
    private String method;                  // 接口方法名
    private String sendTime;                // yyyy-MM-dd HH:mm:ss
    private String signBody = "";
    private String version = "2.0";
    private String serialNo;


    public static HkRequestHead create() {
        return new HkRequestHead();
    }

    public HkRequestHead appCode(String appCode) {
        this.appCode = appCode;
        return this;
    }

    public HkRequestHead format(String format) {
        this.format = format;
        return this;
    }

    public HkRequestHead isSign(String isSign) {
        this.isSign = isSign;
        return this;
    }

    public HkRequestHead method(String method) {
        this.method = method;
        return this;
    }

    public HkRequestHead sendTime(String sendTime) {
        this.sendTime = sendTime;
        return this;
    }

    public HkRequestHead signBody(String signBody) {
        this.signBody = signBody;
        return this;
    }

    public HkRequestHead version(String version) {
        this.version = version;
        return this;
    }

    public HkRequestHead serialNo(String serialNo) {
        this.serialNo = serialNo;
        return this;
    }




}
