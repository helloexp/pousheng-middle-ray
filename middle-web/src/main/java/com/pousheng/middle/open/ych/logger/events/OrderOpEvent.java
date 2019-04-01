package com.pousheng.middle.open.ych.logger.events;

import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.order.model.ShopOrder;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 订单操作事件
 * Created by cp on 9/13/17.
 */
public class OrderOpEvent implements Serializable {

    private static final long serialVersionUID = 9030910613215038108L;

    private static final String REQUEST_PATH = "/order";

    @Getter
    private final HttpServletRequest request;

    @Getter
    private final ParanaUser user;

    @Getter
    private final ShopOrder shopOrder;

    @Getter
    private final String operation;

    public OrderOpEvent(HttpServletRequest request, ParanaUser user,
                        ShopOrder shopOrder, String operation) {
        this.request = request;
        this.user = user;
        this.shopOrder = shopOrder;
        this.operation = operation;
    }

    public String getRequestPath() {
        return REQUEST_PATH;
    }
}
