package com.pousheng.middle.web.events.trade;

import lombok.Data;

/**
 * Created by tony on 2017/8/25.
 * pousheng-middle
 */
@Data
public class NotifyHkOrderDoneEvent implements java.io.Serializable {
    private Long shopOrderId;
}
