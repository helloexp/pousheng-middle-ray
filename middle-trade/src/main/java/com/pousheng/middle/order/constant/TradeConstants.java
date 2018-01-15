package com.pousheng.middle.order.constant;

/**
 * 交易模块相关常量
 * Created by songrenfei on 17/3/17
 */
public class TradeConstants {

    //待处理商品数量
    public static final String WAIT_HANDLE_NUMBER = "waitHandleNumber";

    //发货仓ID
    public static final String WAREHOUSE_ID = "warehouseId";

    //发货仓名称
    public static final String WAREHOUSE_NAME = "warehouseName";

    //绩效店铺名称
    public static final String ERP_PERFORMANCE_SHOP_NAME = "erpPerformanceShopName";
    //绩效店铺编码
    public static final String ERP_PERFORMANCE_SHOP_CODE = "erpPerformanceShopCode";
    //下单店铺名称
    public static final String ERP_ORDER_SHOP_NAME = "erpOrderShopName";
    //下单店铺编码
    public static final String ERP_ORDER_SHOP_CODE = "erpOrderShopCode";


    //发货单商品信息
    public static final String SHIPMENT_ITEM_INFO = "shipmentItemInfo";
    //发货单扩展信息
    public static final String SHIPMENT_EXTRA_INFO = "shipmentExtraInfo";


    //逆向单商品信息
    public static final String REFUND_ITEM_INFO = "refundItemInfo";
    //逆向单换货商品信息
    public static final String REFUND_CHANGE_ITEM_INFO = "refundChangeItemInfo";
    //逆向单丢件补发商品信息
    public static final String REFUND_LOST_ITEM_INFO="refundLostItemInfo";
    //逆向单商品信息
    public static final String REFUND_EXTRA_INFO = "refundExtraInfo";
    //通知电商平台状态
    public static final String ECP_ORDER_STATUS = "ecpOrderStatus";
    //冗余的shipmentId
    public static final String ECP_SHIPMENT_ID ="ecpShipmentId";
    //从电商平台拉取消子单时,子单取消失败,需要将取消失败的子单冗余进总单的extra
    public static final String SKU_CODE_CANCELED="skuCodeCanceled";
    //同步恒康响应头
    public static final String HK_RESPONSE_HEAD ="head";
    //同步恒康发货单返回的body
    public static final String HK_SHIPMENT_ORDER_BODY="orderBody";
    //同步恒康售后单返回的body
    public static final String SYNC_HK_REFUND_BODY="refundBody";
    //发票类型
    public static final String INVOICE_TYPE="type";
    //发票抬头类型
    public static final String INVOICE_TITLE_TYPE="titleType";
    //sku商品初始价格
    public static final String SKU_PRICE="skuPrice";
    //积分
    public static final String SKU_INTEGRAL="integral";
    //客服备注
    public static final String CUSTOMER_SERVICE_NOTE="customerServiceNote";
    //恒康售后单id
    public static final String HK_REFUND_ID="hkRefundId";
    //京东物流编码
    public static final String JD_VEND_CUST_ID="JDCOD";
    //自选物流编码
    public static final String OPTIONAL_VEND_CUST_ID="ZX000001";
    //子单分拆优惠
    public static final String SKU_SHARE_DISCOUNT="shareDiscount";
    //订单支付信息
    public static final String ORDER_PAYMENT_INFO="paymentInfo";
    //外部电商商品id
    public static final String MIDDLE_OUT_ITEM_ID="outItemId";
    //判断售后单是否完善的标记位(有这个标记位则说明可以自动审核,默认0)
    public static final String MIDDLE_REFUND_COMPLETE_FLAG="refundCompleteFlag";
    //hk绩效店铺名称
    public static final String HK_PERFORMANCE_SHOP_NAME="hkPerformanceShopName";
    //hk绩效店铺代码
    public static final String HK_PERFORMANCE_SHOP_CODE="hkPerformanceShopCode";
    //中台换货收货地址
    public static final String MIDDLE_CHANGE_RECEIVE_INFO="middleChangeReceiveInfo";
    //默认退货藏id
    public static final String DEFAULT_REFUND_WAREHOUSE_ID="defaultReWarehouseId";
    //默认退货仓名称
    public static final String DEFAULT_REFUND_WAREHOUSE_NAME="defaultReWarehouseName";
    //默认退货仓对应的外码
    public static final String DEFAULT_REFUND_OUT_WAREHOUSE_CODE="defaultReWarehouseCode";
    //公司代码(账套)
    public static final String HK_COMPANY_CODE="companyCode";
    //不自动生成发货单的备注
    public static final String NOT_AUTO_CREATE_SHIPMENT_NOTE="shipmentNote";
    /**
     * 逆向单来源
     * see RefundSource
     */
    public static final String REFUND_SOURCE = "refundSource";
    /**
     * 是否是预售订单
     */
    public static final String IS_STEP_ORDER="isStepOrder";
    /**
     * 预售订单状态 1.付完定金没有付尾款,2.付完定金和尾款
     */
    public static final String STEP_ORDER_STATUS="stepOrderStatus";


    /**
     * 售中逆向单 取消 处理结果
     * 0 待取消 1已取消
     */
    public static final String REFUND_WAIT_CANCEL = "0";
    public static final String REFUND_CANCELED = "1";

    //活动商品
    public static final String ACTIVITY_ITEM="activityItems";
    //赠品
    public static final String GIFT_ITEM="giftItems";

    public static final String ACTIVITY_SHOP="activityShops";

    //活动赠品id
    public static final String  GIFT_ACTIVITY_ID= "giftActivityId";
    //活动赠品名称
    public static final String  GIFT_ACTIVITY_NAME="giftActivityName";
    //sku订单中的shipmentId
    public static final String SKU_ORDER_SHIPMENT_ID="skuShipmentId";
    //店铺订单选择的快递单号
    public static final String SHOP_ORDER_HK_EXPRESS_CODE="orderHkExpressCode";
    public static final String SHOP_ORDER_HK_EXPRESS_NAME="orderHkExpressName";
    public static final String SKU_ORDER_CANCEL_REASON="skuOrderCancelReason";
    public static final String SHOP_ORDER_CANCEL_REASON="shopOrderCancelReason";
    //货号
    public static final String HK_MATRIAL_CODE="materialCode";

    //电商销售单
    public static final String YYEDI_BILL_TYPE_ON_LINE="SalesBC";
    //线下门店
    public static final String YYEDI_BILL_TYPE_OFF_LINE="Offline";
    //退货类型
    public static final String YYEDI_BILL_TYPE_RETURN ="SalesReturnBC";
    //yyedi返回结果:整体成功
    public static final String  YYEDI_RESPONSE_CODE_SUCCESS="200";
    //yyedi返回结果:部分成功
    public static final String  YYEDI_RESPONSE_CODE_NOT_ALL_SUCCESS = "100";
    //yyedi返回结果:整体失败
    public static final String  YYEDI_RESPONSE_CODE_FAILED = "-100";
    public static final String  ERP_SYNC_TYPE="erpSyncType";
}
