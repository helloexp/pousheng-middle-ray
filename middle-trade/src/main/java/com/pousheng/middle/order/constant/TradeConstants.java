package com.pousheng.middle.order.constant;

/**
 * 交易模块相关常量
 * Created by songrenfei on 17/3/17
 */
public class TradeConstants {

    public static final String activityCode = "code";

    /**
     * 待处理商品数量
     */
    public static final String WAIT_HANDLE_NUMBER = "waitHandleNumber";

    /**
     * 发货仓ID
     */
    public static final String WAREHOUSE_ID = "warehouseId";

    /**
     * 发货仓名称
     */
    public static final String WAREHOUSE_NAME = "warehouseName";

    /**
     * 绩效店铺名称
     */
    public static final String ERP_PERFORMANCE_SHOP_NAME = "erpPerformanceShopName";
    /**
     * 绩效店铺编码
     */
    public static final String ERP_PERFORMANCE_SHOP_CODE = "erpPerformanceShopCode";
    /**
     * 下单店铺名称
     */
    public static final String ERP_ORDER_SHOP_NAME = "erpOrderShopName";
    /**
     * 下单店铺编码
     */
    public static final String ERP_ORDER_SHOP_CODE = "erpOrderShopCode";


    /**
     * 店铺订单ID
     */
    public static final String SHOP_ORDER_ID = "shopOrderId";
    /**
     * 发货单商品信息
     */
    public static final String SHIPMENT_ITEM_INFO = "shipmentItemInfo";
    /**
     * 发货单扩展信息
     */
    public static final String SHIPMENT_EXTRA_INFO = "shipmentExtraInfo";
    /**
     * 是否与门店沟通
     */
    public static final String HAS_CHECK_WITH_STORE = "hasCheckWithStore";
    /**
     * 沟通说明
     */
    public static final String CHECK_WITH_STORE_DESC = "checkWithStoreDesc";

    /**
     * 发货单等待处理结果标记位
     */
    public static final String WAIT_CANCEL_RESULT = "waitCancelResult";


    /**
     * wms发货单通过订单取消来撤销发货单的标记
     */
    public static final String SHIPMENT_CANCEL_BY_ORDER = "cancelByOrder";


    /**
     * 逆向单商品信息
     */
    public static final String REFUND_ITEM_INFO = "refundItemInfo";
    /**
     * 逆向单换货商品信息
     */
    public static final String REFUND_CHANGE_ITEM_INFO = "refundChangeItemInfo";
    /**
     * 逆向单丢件补发商品信息
     */
    public static final String REFUND_LOST_ITEM_INFO = "refundLostItemInfo";
    /**
     * 逆向单商品信息
     */
    public static final String REFUND_EXTRA_INFO = "refundExtraInfo";
    /**
     * 订饭派发中心过来的item
     */
    public static final String REFUND_YYEDI_RECEIVED_ITEM_INFO = "yyediReceivedItemInfo";
    /**
     * 通知电商平台状态
     */
    public static final String ECP_ORDER_STATUS = "ecpOrderStatus";
    /**
     * 冗余的shipmentId
     */
    public static final String ECP_SHIPMENT_ID = "ecpShipmentId";
    /**
     * 从电商平台拉取消子单时,子单取消失败,需要将取消失败的子单冗余进总单的extra
     */
    public static final String SKU_CODE_CANCELED = "skuCodeCanceled";
    /**
     * 同步恒康响应头
     */
    public static final String HK_RESPONSE_HEAD = "head";
    /**
     * 同步恒康发货单返回的body
     */
    public static final String HK_SHIPMENT_ORDER_BODY = "orderBody";
    /**
     * 同步恒康售后单返回的body
     */
    public static final String SYNC_HK_REFUND_BODY = "refundBody";
    /**
     * 发票类型
     */
    public static final String INVOICE_TYPE = "type";
    /**
     * 发票抬头类型
     */
    public static final String INVOICE_TITLE_TYPE = "titleType";
    /**
     * sku商品初始价格
     */
    public static final String SKU_PRICE = "skuPrice";
    /**
     * 积分
     */
    public static final String SKU_INTEGRAL = "integral";
    /**
     * 客服备注
     */
    public static final String CUSTOMER_SERVICE_NOTE = "customerServiceNote";
    /**
     * 恒康售后单id
     */
    public static final String HK_REFUND_ID = "hkRefundId";
    /**
     * yyedi售后单id
     */
    public static final String YYEDI_REFUND_ID = "yyediRefundId";
    /**
     * 京东物流编码
     */
    public static final String JD_VEND_CUST_ID = "JDCOD";
    /**
     * 自选物流编码
     */
    public static final String OPTIONAL_VEND_CUST_ID = "ZX000001";
    /**
     * 子单分拆优惠
     */
    public static final String SKU_SHARE_DISCOUNT = "shareDiscount";
    /**
     * 子单分拆积分抵扣金额
     */
    public static final String SKU_PAYMENT_INTEGRAL = "paymentIntegral";
    /**
     * 子单分拆抵扣积分
     */
    public static final String SKU_USED_INTEGRAL = "usedIntegral";
    /**
     * 订单支付信息
     */
    public static final String ORDER_PAYMENT_INFO = "paymentInfo";
    /**
     * 外部电商商品id
     */
    public static final String MIDDLE_OUT_ITEM_ID = "outItemId";
    /**
     * 判断售后单是否完善的标记位(有这个标记位则说明可以自动审核,默认0)
     */
    public static final String MIDDLE_REFUND_COMPLETE_FLAG = "refundCompleteFlag";
    /**
     * 判断售后单是否同步恒康成功
     */
    public static final String VIP_REFUND_SYNC_FLAG = "vipRefundSyncVipResult";

    /**
     * hk绩效店铺名称
     */
    public static final String HK_PERFORMANCE_SHOP_NAME = "hkPerformanceShopName";
    /**
     * hk绩效店铺代码
     */
    public static final String HK_PERFORMANCE_SHOP_CODE = "hkPerformanceShopCode";
    /**
     * hk绩效店铺外码
     */
    public static final String HK_PERFORMANCE_SHOP_OUT_CODE = "hkPerformanceShopOutCode";
    /**
     * 中台换货收货地址
     */
    public static final String MIDDLE_CHANGE_RECEIVE_INFO = "middleChangeReceiveInfo";
    /**
     * 默认退货仓id
     */
    public static final String DEFAULT_REFUND_WAREHOUSE_ID = "defaultReWarehouseId";
    /**
     * 默认退货仓名称
     */
    public static final String DEFAULT_REFUND_WAREHOUSE_NAME = "defaultReWarehouseName";
    /**
     * 默认退货仓对应的外码
     */
    public static final String DEFAULT_REFUND_OUT_WAREHOUSE_CODE = "defaultReWarehouseCode";
    /**
     * 公司代码(账套)
     */
    public static final String HK_COMPANY_CODE = "companyCode";
    /**
     * 不自动生成发货单的备注
     */
    public static final String NOT_AUTO_CREATE_SHIPMENT_NOTE = "shipmentNote";
    /**
     * 逆向单来源
     * see RefundSource
     */
    public static final String REFUND_SOURCE = "refundSource";

    /**
     * 发货单 换转退标记
     * see RefundSource
     */
    public static final String EXCHANGE_REFUND = "exchangeToRefund";

    /**
     * 是否是预售订单
     */
    public static final String IS_STEP_ORDER = "isStepOrder";
    /**
     * 预售订单状态 1.付完定金没有付尾款,2.付完定金和尾款
     */
    public static final String STEP_ORDER_STATUS = "stepOrderStatus";


    /**
     * 售中逆向单 取消 处理结果
     * 0 待取消 1已取消
     */
    public static final String REFUND_WAIT_CANCEL = "0";
    public static final String REFUND_CANCELED = "1";

    /**
     * 换货售后单添加锁标识
     */
    public static final String AFTER_SALE_EXHCANGE_UN_LOCK = "UNLOCK";
    public static final String AFTER_SALE_EXHCANGE_LOCK = "LOCK";

    /**
     * 活动商品
     */
    public static final String ACTIVITY_ITEM = "activityItems";
    /**
     * 赠品
     */
    public static final String GIFT_ITEM = "giftItems";

    public static final String ACTIVITY_SHOP = "activityShops";

    /**
     * mpos 状态
     */
    /**
     * 发货单接单,待发货
     */
    public static final String MPOS_SHIPMENT_WAIT_SHIP = "1";
    /**
     * 发货单呼叫快递
     */
    public static final String MPOS_SHIPMENT_CALL_SHIP = "2";
    /**
     * 发货单拒单
     */
    public static final String MPOS_SHIPMENT_REJECT = "-1";
    /**
     * 发货单发货,待收货
     */
    public static final String MPOS_SHIPMENT_SHIPPED = "3";
    /**
     * 店铺发货
     */
    public static final String MPOS_SHOP_DELIVER = "1";
    /**
     * 仓库发货
     */
    public static final String MPOS_WAREHOUSE_DELIVER = "2";

    /**
     * 活动赠品id
     */
    public static final String GIFT_ACTIVITY_ID = "giftActivityId";
    /**
     * 活动赠品名称
     */
    public static final String GIFT_ACTIVITY_NAME = "giftActivityName";
    /**
     * sku订单中的shipmentId
     */
    public static final String SKU_ORDER_SHIPMENT_ID = "skuShipmentId";
    /**
     * sku订单中的shipmentCode
     */
    public static final String SKU_ORDER_SHIPMENT_CODE = "skuShipmentCode";
    /**
     * 没有有效的订单
     */
    public static final String YYEDI_RESPONSE_NOT_EXIST_ORDER = "-120";
    /**
     * 店铺订单选择的快递单号
     */
    public static final String SHOP_ORDER_HK_EXPRESS_CODE = "orderHkExpressCode";
    public static final String SHOP_ORDER_HK_EXPRESS_NAME = "orderHkExpressName";
    public static final String SKU_ORDER_CANCEL_REASON = "skuOrderCancelReason";
    public static final String SHOP_ORDER_CANCEL_REASON = "shopOrderCancelReason";
    /**
     * sizeId
     */
    public static final String HK_SIZE_ID = "sizeId";
    /**
     * 是否指定门店 1 指定 2 未指定
     */
    public static final String IS_ASSIGN_SHOP = "isAssignShop";
    /**
     * 指定门店id
     */
    public static final String ASSIGN_SHOP_ID = "assignShopId";
    /**
     * 1 门店发货 2 门店自提
     */
    public static final String IS_SINCE = "isSince";
    /**
     * 订单取消
     */
    public static final String ORDER_CANCEL = "order";
    /**
     * 发货单取消
     */
    public static final String SHIPMENT_CANCEL = "shipment";
    /**
     * 仓库安全库存
     */
    public static final String WAREHOUSE_SAFESTOCK = "safeStock";
    /**
     * 仓库虚拟店编码
     */
    public static final String WAREHOUSE_VIRTUALSHOPCODE = "virtualShopCode";
    /**
     * 仓库虚拟店名称
     */
    public static final String WAREHOUSE_VIRTUALSHOPNAME = "virtualShopName";
    /**
     * 仓库退货仓id
     */
    public static final String WAREHOUSE_RETURNWAREHOUSEID = "returnWarehouseId";
    /**
     * 仓库退货仓编码
     */
    public static final String WAREHOUSE_RETURNWAREHOUSECODE = "returnWarehouseCode";
    /**
     * 仓库退货仓名称
     */
    public static final String WAREHOUSE_RETURNWAREHOUSENAME = "returnWarehouseName";
    /**
     * mpos接单员工
     */
    public static final String MPOS_RECEIVE_STAFF = "mposReceiceStaff";
    /**
     * mpos拒绝原因
     */
    public static final String MPOS_REJECT_REASON = "mposRejectReason";
    /**
     * 快递单号
     */
    public static final String SHIP_SERIALNO = "shipmentSerialNo";
    /**
     * 快递代码
     */
    public static final String SHIP_CORP_CODE = "shipmentCorpCode";
    /**
     * 发货时间
     */
    public static final String SHIP_DATE = "shipmentDate";
    /**
     * 同步无法派单产品失败
     */
    public static final Integer FAIL_NOT_DISPATCHER_SKU_TO_MPOS = 1;
    /**
     * 同步退货单收货失败
     */
    public static final Integer FAIL_REFUND_RECEIVE_TO_MPOS = 2;
    /**
     * 同步发货单pos信息给恒康失败
     */
    public static final Integer FAIL_SYNC_POS_TO_HK = 3;
    /**
     * 同步发货单收货给恒康失败
     */
    public static final Integer FAIL_SYNC_SHIPMENT_CONFIRM_TO_HK = 4;
    /**
     * 同步退货单给恒康失败
     */
    public static final Integer FAIL_SYNC_REFUND_TO_HK = 5;

    /**
     * 同步退货单pos给恒康失败
     */
    public static final Integer FAIL_SYNC_REFUND_POS_TO_HK = 6;

    /**
     * 订单派发中心同步发货状态
     */
    public static final Integer YYEDI_SHIP_NOTIFICATION = 10;

    /**
     * 同步拒收单给恒康失败
     */
    public static final Integer FAIL_SYNC_SALE_REFUSE_TO_HK = 11;
    /**
     * 货号
     */
    public static final String HK_MATRIAL_CODE = "materialCode";

    /**
     * 电商销售单
     */
    public static final String YYEDI_BILL_TYPE_ON_LINE = "SalesBC";
    /**
     * 线下门店
     */
    public static final String YYEDI_BILL_TYPE_OFF_LINE = "Offline";
    /**
     * 退货类型
     */
    public static final String YYEDI_BILL_TYPE_RETURN = "SalesReturnBC";
    /**
     * yyedi返回结果:整体成功
     */
    public static final String YYEDI_RESPONSE_CODE_SUCCESS = "00000";
    /**
     * yyedi返回结果:整体失败
     */
    public static final String YYEDI_RESPONSE_CODE_FAILED = "10100";
    /**
     * yyedi返回结果:取消中
     */
    public static final String YYEDI_RESPONSE_CODE_ING = "10200";

    public static final String ERP_SYNC_TYPE = "erpSyncType";

    public static final String SKU_CANNOT_BE_DISPATCHED = "该商品无法派出";
    /**
     * 导出订单
     */
    public static final String EXPORT_ORDER = "order";
    /**
     * 导出售后单
     */
    public static final String EXPORT_REFUND = "refund";
    /**
     * 导出发货单
     */
    public static final String EXPORT_SHIPMENT = "shipment";
    /**
     * 导出pos单
     */
    public static final String EXPORT_POS = "pos";
    /**
     * 发货仓限制
     */
    public static final String EXPORT_STOCK_SEND = "stock-send";

    /**
     * 导出逆向无头件
     */
    public static final String EXPORT_REVERSE_HEADLESS = "headless";

    /**
     * 是否参与全渠道店铺 1参与 0 不参与
     */
    public static final String IS_ALL_CHANNEL_SHOP = "isAllChannelShop";


    /**
     * 是否是新的全渠道订单
     */
    public static final String IS_NEW_ALL_CHANNEL_SHOP = "isNewAllChannelShop";

    public static final String IS_HK_POS_ORDER = "isHkPosOrder";
    public static final String SHIPMENT_ID = "shipmentId";
    public static final String ORDER_PREFIX = "SAL";

    public static final String SHIPMENT_PREFIX = "SHP";

    public static final String REFUND_PREFIX = "ASS";


    /**
     * 数据逻辑状态  1可用 1已删除
     */
    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = -1;

    public static final String SHIPMENT_EXPRESS_NODE_DETAILS = "shipmentExpressNodeDetail";
    /**
     * mpos需要额外记录到中台的字段
     */
    /**
     * 圆通回传快递单号
     */
    public static final String YTO_CALL_BACK_MAIL_NO = "callbackMailNo";

    /**
     * 物流单号生成规则
     */
    public static final String EXPRESS_ORDER_ID = "expressOrderId";
    /**
     * 平台出资折扣
     */
    public static final String PLATFORM_DISCOUNT_FOR_SHOP = "platformDiscount";
    /**
     * 分摊到子订单上的平台出资折扣
     */
    public static final String PLATFORM_DISCOUNT_FOR_SKU = "skuPlatformDiscount";
    /**
     * 拉取映射关系
     */
    public static final String ITEM_MAPPING_STOCK = "itemMappingStock";
    /**
     * 淘宝C店
     */
    public static final String IS_TAOBAO_SHOP = "isTaobaoShop";
    /**
     * 第三方换货拉取标记： 值Y/N
     */
    public static final String EXCHANGE_PULL = "exchangePull";
    /**
     * 派单规则：同公司账套优先/优先发货优先
     */
    public static final String IS_NEW_DISPATCH_ORDER_LOGIC = "isNewDispatchOrderLogic";
    /**
     * 手动派单只限默认发货仓：是/否
     */
    public static final String MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG = "manualShipmentCheckWarehouseFlag";


    /**
     * 共享仓标记
     */
    public static final String IS_SHARED_STOCK = "sharedStock";
    /**
     * 宝胜中台mq消费group
     */
    public static final String POUSHENG_MIDDLE_MQ_CONSUMER_GROUP = "pousheng-middle-consumer-group";
    /**
     * CompensateBiz业务处理topic
     */
    public static final String POUSHENG_MIDDLE_COMPENSATE_BIZ_TOPIC = "pousheng-middle-compensate-biz-topic";
    /**
     * mpos发货单被拒单的需要释放库存的发货单集合
     */
    public static final String REJECT_SHIPMENT_OCCUPY_LIST = "rejectShipmentOccupyList";
    /**
     * 有备注的订单处理标识
     */
    public static final String ORDER_NOTE_PROCESS_FLAG = "orderNoteProcessFlag";

    /**
     * 云聚JIT单据类型
     */
    public static final String YUN_JU_JIT_BILL_TYPE = "Sal_Ship";

    /**
     * skx售后发货单挂起标识
     */
    public static final String SKX_REFUND_FREEZE_FLAG = "skxFreeze";

    /**
     * 解析失败，模糊地址
     */
    public static final String VAUGE_ADDRESS = "vagueAddress";

    /**
     * 通知vip退货单信息失败
     */
    public static final Integer FAIL_REFUND_TO_VIP = 71;

    /**
     * 通知天猫换货单信息失败（仓库收到退货）
     */
    public static final Integer FAIL_REFUND_TO_TMALL = 72;

    /**
     * 通知天猫换货单发货信息失败（仓库发出换货商品）
     */
    public static final Integer FAIL_REFUND_SHIP_TO_TMALL = 73;

    /**
     * jit订单缺货同步云聚失败
     */
    public static final Integer FAIL_SYNC_OUTOFSTOCK_JIT_ORDER_TO_YJ = 74;

    public static final String IS_CUSTOMER_PICK_UP = "is_customer_pick_up";
    /**
     * 退货仓账套校验（宝唯）
     */
    public static final Integer BAO_WEI_COMPANY_ID = 325;

    /**
     * 虚仓编码
     */
    public static final String VISUAL_WAREHOUSE_CODE = "visualWarehouseCode";

	/**
	 * XXX POUS923 RAY 2019.05.07: 如果第三方有傳入SkuCode但是
	 * 1.parana_item_mappings沒有對應
	 * 2.parana_sku_templates沒有對應的。在extra_json中的KEY
	 */
	public static final String THIRD_PARTY_SKU_CODE = "originSkuCode";
    public static  final  String PULL_REFUND_EXCHANGE_FLAG_KEY="checkStatusWhenPullRefundAndExchange";
    public static  final  String PULL_REFUND_EXCHANGE_FLAG_VALUE="1";


    public static final int MAX_REFUND_FLAG=127;
    /**
     * 拉取待上架商品
     */
    public static final String UN_SALE_ITEM_FETCH_ENABLED = "unSaleItemFetchEnabled";

    /**
     * 是否管控，分组、发货限制及安全库存
     */
    public static final String MANUAL_DISPATCH_RULE_CONTROL = "manualDispatchRuleControl";
}
