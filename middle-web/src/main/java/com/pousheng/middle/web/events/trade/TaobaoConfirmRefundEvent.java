package com.pousheng.middle.web.events.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/29
 * pousheng-middle
 */
@Data
public class TaobaoConfirmRefundEvent implements Serializable {
    /**
     * 第三方平台售后单号
     */
    private String  openAfterSaleId;

    /**
     * 售后订单号
     */
    private Long refundId;
    /**
     * 来源渠道
     */
    private String  channel;

    /**
     * 店铺id
     */
    private Long openShopId;
}
