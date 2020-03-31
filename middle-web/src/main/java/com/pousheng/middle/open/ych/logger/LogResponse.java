package com.pousheng.middle.open.ych.logger;

import lombok.Data;

/**
 * Created by cp on 9/13/17.
 */
@Data
public class LogResponse {

    private String result;

    private String errMsg;

    public boolean isSuccess() {
        return "success".equals(result);
    }

}
