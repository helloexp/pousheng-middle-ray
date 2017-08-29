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

    /**
     * 逆向单来源
     * see RefundSource
     */
    public static final String REFUND_SOURCE = "refundSource";



    /**
     * 售中逆向单 取消 处理结果
     * 0 待取消 1已取消
     */
    public static final String REFUND_WAIT_CANCEL = "0";
    public static final String REFUND_CANCELED = "1";


}
