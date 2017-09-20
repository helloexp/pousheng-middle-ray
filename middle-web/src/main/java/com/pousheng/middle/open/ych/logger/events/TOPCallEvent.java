package com.pousheng.middle.open.ych.logger.events;

import lombok.Getter;

import java.io.Serializable;

/**
 * TOP调用事件
 * Created by cp on 9/13/17.
 */
public class TOPCallEvent implements Serializable {

    private static final long serialVersionUID = 9030910613215038108L;

    private static final String REQUEST_PATH = "/top";

    @Getter
    private final String url;

    public TOPCallEvent(String url) {
        this.url = url;
    }

    public String getRequestPath() {
        return REQUEST_PATH;
    }
}
