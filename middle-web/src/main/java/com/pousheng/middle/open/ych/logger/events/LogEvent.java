package com.pousheng.middle.open.ych.logger.events;

import lombok.Getter;

/**
 * Created by cp on 9/13/17.
 */
public abstract class LogEvent {

    @Getter
    private final String ip;

    public LogEvent(String ip) {
        this.ip = ip;
    }

    public abstract String getRequestPath();

}
