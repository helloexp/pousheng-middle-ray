package com.pousheng.middle.open.ych.logger.events;

import lombok.Getter;

import java.io.Serializable;

/**
 * 订单发送事件
 * Created by cp on 9/13/17.
 */
public class OrderSendEvent extends LogEvent implements Serializable {

    private static final long serialVersionUID = 9030910613215038108L;

    private static final String REQUEST_PATH = "/sendOrder";

    @Getter
    private final String tradeIds;

    @Getter
    private final String sendTo;

    @Getter
    private final String url;

    public OrderSendEvent(String ip, String tradeIds,
                          String sendTo, String url) {
        super(ip);
        this.tradeIds = tradeIds;
        this.sendTo = sendTo;
        this.url = url;
    }

    @Override
    public String getRequestPath() {
        return REQUEST_PATH;
    }
}
