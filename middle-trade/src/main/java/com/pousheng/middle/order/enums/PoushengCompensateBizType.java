package com.pousheng.middle.order.enums;

import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */

public enum PoushengCompensateBizType {
    /**
     * 通知恒康
     */
    NOTIFY_HK,

    /**
     * YYEDI回传发货信息
     */
    YYEDI_SYNC_SHIPMENT_RESULT,

    /**
     * 通知恒康发货单时间
     */
    NOTIFY_HK_ORDER_DOWN,

    /**
     * YYEDI回传售后单信息
     */

    YYEDI_SYNC_REFUND_RESULT,

    /**
     * 第三方售后单状态查询任务
     */
    THIRD_REFUND_RESULT,

    /**
     * 天猫订单生成发货单
     */
    TMALL_ORDER_CREATE_SHIP;

}
