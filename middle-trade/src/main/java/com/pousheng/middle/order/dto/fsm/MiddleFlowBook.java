package com.pousheng.middle.order.dto.fsm;

import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.dto.fsm.OrderStatus;

/**
 * Created by songrenfei on 2017/6/12
 */
public class MiddleFlowBook {


    /**
     * 订单流程
     */
    public static final Flow orderFlow = new Flow("orderFlow") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {
            //===========正向流程
            //待处理 -->处理 -> 待发货
            addTransition(MiddleOrderStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.HANDLE.toOrderOperation(),
                    MiddleOrderStatus.WAIT_SHIP.getValue());

            //待发货 -->发货 -> 商家已发货
            addTransition(MiddleOrderStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP.toOrderOperation(),
                    MiddleOrderStatus.SHIPPED.getValue());

            //商家已发货 -->确认收货 --> 交易完成
            addTransition(MiddleOrderStatus.SHIPPED.getValue(),
                    MiddleOrderEvent.CONFIRM.toOrderOperation(),
                    MiddleOrderStatus.CONFIRMED.getValue());


            //在付款后发货前, 买家可申请退款, 商家同意退款
            //买家申请退款 -->审核通过 -> 商家同意退款
            addTransition(MiddleOrderStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue());

            //商家同意退款 -->同步hk -> 同步成功
            addTransition(MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleOrderStatus.REFUND_SYNC_HK_SUCCESS.getValue());

            //同步成功 -->退款 -> 已退款
            addTransition(MiddleOrderStatus.REFUND_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.REFUND.toOrderOperation(),
                    MiddleOrderStatus.REFUND.getValue());
        }
    };


    /**
     * 发货单流程
     */
    public static final Flow shipmentsFlow = new Flow("shipmentsFlow") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {
            //===========正向流程
            //待同步hk -->同步hk -> 同步中
            addTransition(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_ING.getValue());

            //同步中 -->同步成功 -> 同步成功，待发货
            addTransition(MiddleShipmentsStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation(),
                    MiddleShipmentsStatus.WAIT_SHIP.getValue());

            //同步失败 -->同步失败 -> 同步失败
            addTransition(MiddleShipmentsStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_FAIL.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_FAIL.getValue());

            //同步失败 -->同步 -> 同步中
            addTransition(MiddleShipmentsStatus.SYNC_HK_FAIL.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_ING.getValue());

            //待发货 -->发货 -> 商家已发货,待同步电商平台
            addTransition(MiddleShipmentsStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP.toOrderOperation(),
                    MiddleShipmentsStatus.SHIPPED_WAIT_SYNC_ECP.getValue());

            //待同步电商平台 -->同步 --> 同步中
            addTransition(MiddleShipmentsStatus.SHIPPED_WAIT_SYNC_ECP.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_ECP_ING.getValue());

            //同步中 -->同步成功 --> 待收货
            addTransition(MiddleShipmentsStatus.SYNC_ECP_ING.getValue(),
                    MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_ECP_SUCCESS_WAIT_RECEIVED.getValue());

            //同步中 -->同步失败 --> 同步失败
            addTransition(MiddleShipmentsStatus.SYNC_ECP_ING.getValue(),
                    MiddleOrderEvent.SYNC_FAIL.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_ECP_FAIL.getValue());

            //同步失败 -->同步 --> 同步中
            addTransition(MiddleShipmentsStatus.SYNC_ECP_FAIL.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_ECP_ING.getValue());


            //在付款后发货前, 买家可申请退款, 商家也可主动退款
            //买家已支付 -->申请退款 -> 买家申请退款
            addTransition(OrderStatus.PAID.getValue(),
                    OrderEvent.REFUND_APPLY.toOrderOperation(),
                    OrderStatus.REFUND_APPLY.getValue());
            //买家申请退款 -->审核通过 -> 商家同意退款
            addTransition(OrderStatus.REFUND_APPLY.getValue(),
                    OrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    OrderStatus.REFUND_APPLY_AGREED.getValue());
        }
    };



    /**
     * 售后单流程
     */
    public static final Flow afterSalesFlow = new Flow("afterSalesFlow") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {

            //===========正向流程
            //待处理 -->处理 -> 待同步恒康
            addTransition(MiddleRefundStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.HANDLE.toOrderOperation(),
                    MiddleRefundStatus.WAIT_SYNC_HK.getValue());
            //待同步恒康 -->同步 -> 同步中
            addTransition(MiddleRefundStatus.WAIT_SYNC_HK.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_ING.getValue());

            //同步中 -->同步退款成功 --> 同步退款成功-待退款
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue());

            //同步退款成功-待退款 -->退款 --> 退款成功
            addTransition(MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.REFUND.toOrderOperation(),
                    MiddleRefundStatus.REFUND.getValue());

            //同步中 -->同步退货成功 --> 同步退货成功-待退货
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue());

            //同步退货成功-待退货 -->退货-完成 --> 已退货待退款
            addTransition(MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.RETURN.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_REFUND.getValue());

            //同步退货成功-待退货 -->换货退货-完成 --> 已退货待创建发货
            addTransition(MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.RETURN_CHANGE.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue());

            //已退货待创建发货 -->创建 --> 待发货
            addTransition(MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue(),
                    MiddleOrderEvent.CREATE_SHIPMENT.toOrderOperation(),
                    MiddleRefundStatus.WAIT_SHIP.getValue());

            //待发货 -->运营发货 --> 待确认收货
            addTransition(MiddleRefundStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP_ADMIN.toOrderOperation(),
                    MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue());

            //待确认收货 -->运营确认收货 --> 已完成
            addTransition(MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue(),
                    MiddleOrderEvent.CONFIRM.toOrderOperation(),
                    MiddleRefundStatus.DONE.getValue());


            //已退货待退款 -->退款 --> 已退款
            addTransition(MiddleRefundStatus.RETURN_DONE_WAIT_REFUND.getValue(),
                    MiddleOrderEvent.REFUND.toOrderOperation(),
                    MiddleRefundStatus.REFUND.getValue());

            //同步中 -->同步失败 --> 同步失败
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_FAIL.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_FAIL.getValue());


            //同步失败 -->同步 --> 同步中台
            addTransition(MiddleRefundStatus.SYNC_HK_FAIL.getValue(),
                    MiddleOrderEvent.SYNC.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_ING.getValue());

        }
    };
}
