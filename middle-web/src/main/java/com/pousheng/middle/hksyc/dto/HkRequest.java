package com.pousheng.middle.hksyc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Description: add something here
 * User: xiao
 * Date: 03/07/2017
 */
@Data
public class HkRequest implements Serializable {
    private static final long serialVersionUID = 1330207202634462274L;

    private HkRequestHead head;
    private Map<String, Object> body;

    public static HkRequest create() {
        return new HkRequest();
    }

    public HkRequest head(HkRequestHead head) {
        this.head = head;
        return this;
    }

    public HkRequest body(Map<String, Object> body) {
        this.body = body;
        return this;
    }




}
