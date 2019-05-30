package com.pousheng.middle.order.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author bernie
 * @date 2019/5/6
 */
@Data
public class MiddleOrderRefundDto implements Serializable {

    /**
     * 退款单id
     */
    private Long refundId;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * 外部订单号
     */
    private String orderOutId;

    /**
     * 店铺id
     */
    private Long shopId;
    /**
     * 当前状态
     */
    private Integer currentStatus;
}
