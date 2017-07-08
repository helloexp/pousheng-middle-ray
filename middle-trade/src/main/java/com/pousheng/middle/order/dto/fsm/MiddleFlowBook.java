package com.pousheng.middle.order.dto.fsm;

import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import io.terminus.parana.order.dto.fsm.Flow;

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
            //待处理 -->处理 -> 处理中
            addTransition(MiddleOrderStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.HANDLE.toOrderOperation(),
                    MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue());

            //处理中 -->处理 -> 处理中
            addTransition(MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                    MiddleOrderEvent.HANDLE.toOrderOperation(),
                    MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue());

            //处理中 -->处理完成 -> 待发货
            addTransition(MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                    MiddleOrderEvent.HANDLE_DONE.toOrderOperation(),
                    MiddleOrderStatus.WAIT_SHIP.getValue());


            //待发货 -->发货 -> 商家已发货
            addTransition(MiddleOrderStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP.toOrderOperation(),
                    MiddleOrderStatus.SHIPPED.getValue());

            //商家已发货 -->确认收货 --> 交易完成
            addTransition(MiddleOrderStatus.SHIPPED.getValue(),
                    MiddleOrderEvent.CONFIRM.toOrderOperation(),
                    MiddleOrderStatus.CONFIRMED.getValue());


            //待处理 -->取消 -> 已取消
            addTransition(MiddleOrderStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.CANCEL.toOrderOperation(),
                    MiddleOrderStatus.CANCEL.getValue());


            //在付款后发货前, 买家可申请退款, 商家同意退款
            //待全部处理完成 -->审核通过 -> 商家同意退款
            addTransition(MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue(),
                    MiddleOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue());


            //待发货 -->审核通过 -> 商家同意退款
            addTransition(MiddleOrderStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue());

            //商家同意退款 -->同步hk -> 同步成功
            addTransition(MiddleOrderStatus.REFUND_APPLY_WAIT_SYNC_HK.getValue(),
                    MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation(),
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
                    MiddleOrderEvent.SYNC_HK.toOrderOperation(),
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
                    MiddleOrderEvent.SYNC_HK.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_ING.getValue());

            //待发货 -->发货 -> 商家已发货,待同步电商平台
            addTransition(MiddleShipmentsStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP.toOrderOperation(),
                    MiddleShipmentsStatus.SHIPPED_WAIT_SYNC_ECP.getValue());

            //待同步电商平台 -->同步 --> 同步中
            addTransition(MiddleShipmentsStatus.SHIPPED_WAIT_SYNC_ECP.getValue(),
                    MiddleOrderEvent.SYNC_ECP.toOrderOperation(),
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
                    MiddleOrderEvent.SYNC_ECP.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_ECP_ING.getValue());

            //===========逆向流程

            //在付款后发货前, 买家可申请退款
            //买家已支付 -->申请退款 -> 买家申请退款

            //待处理 -->取消 -> 已取消（不需同步恒康）
            addTransition(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue(),
                    MiddleOrderEvent.CANCEL.toOrderOperation(),
                    MiddleShipmentsStatus.CANCELED.getValue());

            //待发货 -->取消恒康 -> 取消同步中
            addTransition(MiddleShipmentsStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());

            //同步失败 -->取消恒康 -> 同步中
            addTransition(MiddleShipmentsStatus.SYNC_HK_FAIL.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue());


            //同步取消退货中 -->取消成功--> 已取消
            addTransition(MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue(),
                    MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation(),
                    MiddleShipmentsStatus.CANCELED.getValue());

            //同步取消退货中 -->取消失败--> 取消同步失败
            addTransition(MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue(),
                    MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_CANCEL_FAIL.getValue());

            //取消同步失败 -->取消--> 同步恒康取消中
            addTransition(MiddleShipmentsStatus.SYNC_HK_CANCEL_FAIL.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue());

            //目前没有添加同步取消失败状态下再次同步恒康正向单据的操作（如果要加的话，待发货和同步发货单失败两个状态下的取消要分开，
            //只有同步发货单失败的取消的失败才可以出现再次同步正向的单据操作）

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
                    MiddleOrderEvent.SYNC_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_ING.getValue());

            //同步中 -->同步失败 --> 同步失败
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_FAIL.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_FAIL.getValue());

            //同步售后单失败 -->同步 --> 同步中
            addTransition(MiddleRefundStatus.SYNC_HK_FAIL.getValue(),
                    MiddleOrderEvent.SYNC_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_ING.getValue());

            //============== 仅退款 ====================

            //同步中 -->同步退款成功 --> 同步退款成功-待退款
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue());

            //同步退款成功-待退款 -->退款 --> 退款成功
            addTransition(MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.REFUND.toOrderOperation(),
                    MiddleRefundStatus.REFUND.getValue());

            //============== 退货 ====================

            //同步中 -->同步退货成功 --> 同步退货成功-待退货
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue());

            //同步退货成功-待退货 -->退货-完成 --> 已退货待同步电商平台
            addTransition(MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.RETURN.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_SYNC_ECP.getValue());

            //已退货待同步电商 -->同步电商 --> 同步电商中
            addTransition(MiddleRefundStatus.RETURN_DONE_WAIT_SYNC_ECP.getValue(),
                    MiddleOrderEvent.SYNC_ECP.toOrderOperation(),
                    MiddleRefundStatus.SYNC_ECP_ING.getValue());

            //同步电商中 -->同步成功 --> 同步电商成功待退款
            addTransition(MiddleRefundStatus.SYNC_ECP_ING.getValue(),
                    MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.SYNC_ECP_SUCCESS_WAIT_REFUND.getValue());

            //同步电商成功待退款 -->退款 --> 已退款
            addTransition(MiddleRefundStatus.SYNC_ECP_SUCCESS_WAIT_REFUND.getValue(),
                    MiddleOrderEvent.REFUND.toOrderOperation(),
                    MiddleRefundStatus.REFUND.getValue());

            //同步电商中 -->同步失败 --> 同步电商失败
            addTransition(MiddleRefundStatus.SYNC_ECP_ING.getValue(),
                    MiddleOrderEvent.SYNC_FAIL.toOrderOperation(),
                    MiddleRefundStatus.SYNC_ECP_FAIL.getValue());

            //同步电商失败 -->同步 --> 同步电商中
            addTransition(MiddleRefundStatus.SYNC_ECP_FAIL.getValue(),
                    MiddleOrderEvent.SYNC_ECP.toOrderOperation(),
                    MiddleRefundStatus.SYNC_ECP_ING.getValue());


            //=============== 换货 (全部是手动创建，不用和电商交互)==================


            //同步中 -->同步换货成功 --> 同步换货成功-待退货
            addTransition(MiddleRefundStatus.SYNC_HK_ING.getValue(),
                    MiddleOrderEvent.SYNC_CHANGE_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.CHANGE_SYNC_HK_SUCCESS.getValue());

            //同步换货成功-待退货 -->换货退货-完成 --> 已退货待创建发货
            addTransition(MiddleRefundStatus.CHANGE_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.RETURN_CHANGE.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue());

            //已退货待创建发货 -->创建发货单（换货商品全部生成发货单） --> 待发货
            addTransition(MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue(),
                    MiddleOrderEvent.CREATE_SHIPMENT.toOrderOperation(),
                    MiddleRefundStatus.WAIT_SHIP.getValue());

            //待发货 -->发货（换货的发货单全部发货完成） --> 待确认收货
            addTransition(MiddleRefundStatus.WAIT_SHIP.getValue(),
                    MiddleOrderEvent.SHIP.toOrderOperation(),
                    MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue());

            //待确认收货 -->运营确认收货 --> 已完成
            addTransition(MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue(),
                    MiddleOrderEvent.CONFIRM.toOrderOperation(),
                    MiddleRefundStatus.DONE.getValue());



            //===========逆向流程


            //待处理 -->取消 -> 已取消（不需同步恒康）
            addTransition(MiddleRefundStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.DELETE.toOrderOperation(),
                    MiddleRefundStatus.DELETED.getValue());


            //待处理 -->取消 -> 已取消（不需同步恒康）
            addTransition(MiddleRefundStatus.WAIT_HANDLE.getValue(),
                    MiddleOrderEvent.CANCEL.toOrderOperation(),
                    MiddleRefundStatus.CANCELED.getValue());

            //=============== 仅退款 ================

            //同步退款成功-待退款 -->取消 --> 同步恒康取消中
            addTransition(MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());

            //=============== 退货退款 ================

            //同步退货成功-待退货 --取消 --> 同步取消中
            addTransition(MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());

            //取消同步失败 -->退货-完成 --> 已退货待退款
            addTransition(MiddleRefundStatus.SYNC_HK_CANCEL_FAIL.getValue(),
                    MiddleOrderEvent.RETURN.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_SYNC_ECP.getValue());


            //=============== 换货 ================

            //同步换货成功-待退货 -->取消 --> 同步取消中
            addTransition(MiddleRefundStatus.CHANGE_SYNC_HK_SUCCESS.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());


            //取消同步失败 -->>换货退货-完成 --> 已退货待创建发货
            addTransition(MiddleRefundStatus.SYNC_HK_CANCEL_FAIL.getValue(),
                    MiddleOrderEvent.RETURN_CHANGE.toOrderOperation(),
                    MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue());


            // ============ 公共操作 ==========



            //同步售后单失败 -->取消 --> 同步恒康取消中
            addTransition(MiddleRefundStatus.SYNC_HK_FAIL.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());


            //同步取消退货中 -->取消成功--> 已取消
            addTransition(MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue(),
                    MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation(),
                    MiddleRefundStatus.CANCELED.getValue());

            //同步取消退货中 -->取消失败--> 取消同步失败
            addTransition(MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue(),
                    MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_FAIL.getValue());

            //取消同步失败 -->取消--> 同步恒康取消中
            addTransition(MiddleRefundStatus.SYNC_HK_CANCEL_FAIL.getValue(),
                    MiddleOrderEvent.CANCEL_HK.toOrderOperation(),
                    MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue());






        }
    };
}
