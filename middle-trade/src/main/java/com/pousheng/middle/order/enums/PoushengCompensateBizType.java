package com.pousheng.middle.order.enums;

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
     * 第三方渠道订单生成发货单
     */
    THIRD_ORDER_CREATE_SHIP,

    /**
     * 通知mpos确认收货
     */
    SYNC_MPOS_CONFIRM_DONE,

    /**
     * UNLOCK事件补偿
     */
    UNLOCK_STOCK_EVENT,

    /**
     * LOCK事件补偿
     */
    LOCK_STOCK_EVENT,

    /**
     * 扣减库存
     */
    STOCK_API_DECREASE_STOCK,

    /**
     * SKX同步发货单
     */
    SKX_SYNC_SHIPMENT_RESULT,

    /**
     * 同步发货结果到第三方
     */
    SYNC_ECP,

    /**
     * 批量导入库存推送规则
     */
    IMPORT_SHOP_SKU_RULE,

    /**
     * 导入商品推送比例
     */
    IMPORT_ITEM_PUSH_RATIO,

    /**
     * 异步插入云聚JIT订单
     */
    OUT_OPEN_ORDER,

    /**
     * JIT释放库存
     */
    JIT_UNLOCK_STOCK_API,

    /**
     * JIT拣货单回执
     */
    JIT_ORDER_RECEIPT,

    /**
     *
     */
    JIT_BIG_ORDER,

    /**
     * 批量导入商品供货规则
     */
    IMPORT_ITEM_SUPPLY_RULE

}
