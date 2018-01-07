package com.pousheng.middle.yyedisyc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Description: add something here
 * User: xiao
 * Date: 03/07/2017
 */
@Data
public class YYEdiRequest implements Serializable {
    private static final long serialVersionUID = 1330207202634462274L;

    private YYEdiRequestHead head;
    private Map<String, Object> body;

    public static YYEdiRequest create() {
        return new YYEdiRequest();
    }

    public YYEdiRequest head(YYEdiRequestHead head) {
        this.head = head;
        return this;
    }

    public YYEdiRequest body(Map<String, Object> body) {
        this.body = body;
        return this;
    }




}
