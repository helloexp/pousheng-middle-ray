package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.Data;

import java.io.Serializable;

/**
 * @author bernie
 * @date 2019/6/3
 */
@Data
public  class ReverseLogisticBase  implements Serializable {
    private static final long serialVersionUID = -5820764409819254831L;

    /**
     * 店铺
     */
    private String shop;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 单据状态
     */
    private String status;
}
