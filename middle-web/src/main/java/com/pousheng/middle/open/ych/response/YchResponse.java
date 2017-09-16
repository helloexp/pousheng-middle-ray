package com.pousheng.middle.open.ych.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cp on 9/15/17.
 */
@Data
public class YchResponse implements Serializable {

    private static final long serialVersionUID = -8646471299755914839L;

    private String result;

    private String errMsg;

    public boolean isSuccess() {
        return "success".equals(result);
    }
}
