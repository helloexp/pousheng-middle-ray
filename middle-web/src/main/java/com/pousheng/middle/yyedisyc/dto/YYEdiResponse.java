package com.pousheng.middle.yyedisyc.dto;

import java.io.Serializable;

/**
 * 同步发货单或者售后单时恒康返回消息的消息头
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */

public class YYEdiResponse implements Serializable{
    private static final long serialVersionUID = -4030774668654863201L;
    //200:整体成功,100:部分成功,-100:整体失败
    private String code;

    private String returnJson;

    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReturnJson() {
        return returnJson;
    }

    public void setReturnJson(String returnJson) {
        this.returnJson = returnJson;
    }

}
