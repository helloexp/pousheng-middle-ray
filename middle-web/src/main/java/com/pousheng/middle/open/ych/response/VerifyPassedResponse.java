package com.pousheng.middle.open.ych.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cp on 9/15/17.
 */
@Data
public class VerifyPassedResponse extends YchResponse implements Serializable {

    private static final long serialVersionUID = 6448491214410329570L;

    private String verifyResult;

    public boolean verifyOk() {
        return "success".equals(verifyResult);
    }

}
