package com.pousheng.middle.order.dto.fsm;

import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderEvent;
import io.terminus.parana.order.dto.fsm.OrderStatus;

/**
 * Created by songrenfei on 2017/6/12
 */
public class MiddleFlowBook {


    /**
     * 自营店铺订单流程
     */
    public static final Flow middle = new Flow("middle") {

        /**
         * 配置流程
         */
        @Override
        protected void configure() {
            //===========正向流程
            //待处理 -->处理 -> 待发货
            addTransition(OrderStatus.NOT_PAID.getValue(),
                    OrderEvent.PAY.toOrderOperation(),
                    OrderStatus.PAID.getValue());

            //买家已支付 -->发货 -> 商家已发货
            addTransition(OrderStatus.PAID.getValue(),
                    OrderEvent.SHIP.toOrderOperation(),
                    OrderStatus.SHIPPED.getValue());

            //商家已发货 -->确认收货 --> 交易完成
            addTransition(OrderStatus.SHIPPED.getValue(),
                    OrderEvent.CONFIRM.toOrderOperation(),
                    OrderStatus.CONFIRMED.getValue());

            //在付款之前可以买卖双方均可取消订单
            //待付款 -->买家取消 -> 买家已取消
            addTransition(OrderStatus.NOT_PAID.getValue(),
                    OrderEvent.BUYER_CANCEL.toOrderOperation(),
                    OrderStatus.BUYER_CANCEL.getValue());
            //待付款 -->商家取消 -> 商家已取消
            addTransition(OrderStatus.NOT_PAID.getValue(),
                    OrderEvent.SELLER_CANCEL.toOrderOperation(),
                    OrderStatus.SELLER_CANCEL.getValue());
            //addTransition(0, new OrderOperation("system", "超时关闭"), -3); //待付款 -> 超时取消

            //在付款后发货前, 买家可申请退款, 商家也可主动退款
            //买家已支付 -->申请退款 -> 买家申请退款
            addTransition(OrderStatus.PAID.getValue(),
                    OrderEvent.REFUND_APPLY.toOrderOperation(),
                    OrderStatus.REFUND_APPLY.getValue());
            //买家申请退款 -->审核通过 -> 商家同意退款
            addTransition(OrderStatus.REFUND_APPLY.getValue(),
                    OrderEvent.REFUND_APPLY_AGREE.toOrderOperation(),
                    OrderStatus.REFUND_APPLY_AGREED.getValue());

            //买家申请退款 -->审核拒绝 -> 商家拒绝退款
            addTransition(OrderStatus.REFUND_APPLY.getValue(),
                    OrderEvent.REFUND_APPLY_REJECT.toOrderOperation(),
                    OrderStatus.REFUND_APPLY_REJECTED.getValue());
            //买家申请退款 -->买家撤销退款 -> 买家取消退款
            addTransition(OrderStatus.REFUND_APPLY.getValue(),
                    OrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    OrderStatus.PAID.getValue());
            //商家拒绝退款 -> 买家撤销退款 -> 已付款（parana）
            /*addTransition(OrderStatus.REFUND_APPLY_REJECTED.getValue(),
                    OrderEvent.REFUND_APPLY_CANCEL.toOrderOperation(),
                    OrderStatus.PAID.getValue());*/
            //商家拒绝退款 -> 申请退款 -> 买家申请退款 (pousheng）
            addTransition(OrderStatus.REFUND_APPLY_REJECTED.getValue(),
                    OrderEvent.REFUND_APPLY.toOrderOperation(),
                    OrderStatus.REFUND_APPLY.getValue());
            //商家拒绝退款 -> 发货 -> 商家已发货（pousheng）
            addTransition(OrderStatus.REFUND_APPLY_REJECTED.getValue(),
                    OrderEvent.SHIP.toOrderOperation(),
                    OrderStatus.SHIPPED.getValue());

            //商家同意退款 -->退款 -> 商家已退款
            addTransition(OrderStatus.REFUND_APPLY_AGREED.getValue(),
                    OrderEvent.REFUND.toOrderOperation(),
                    OrderStatus.REFUND.getValue());

            //在发货后, 买家可申请退款退货
            //商家已发货 -> 申请退款退货 --> 买家申请退款退货
            addTransition(OrderStatus.SHIPPED.getValue(),
                    OrderEvent.RETURN_APPLY.toOrderOperation(),
                    OrderStatus.RETURN_APPLY.getValue());

            //买家申请退款退货 -> 审核通过 --> 商家同意退款退货
            addTransition(OrderStatus.RETURN_APPLY.getValue(),
                    OrderEvent.RETURN_APPLY_AGREE.toOrderOperation(),
                    OrderStatus.RETURN_APPLY_AGREED.getValue());

            //商家同意退款退货 -> 审核拒绝 --> 商家拒绝退款退货
            addTransition(OrderStatus.RETURN_APPLY.getValue(),
                    OrderEvent.RETURN_APPLY_REJECT.toOrderOperation(),
                    OrderStatus.RETURN_APPLY_REJECTED.getValue());
            //买家申请退款退货 -> 买家撤销退货申请 --> 已发货
            addTransition(OrderStatus.RETURN_APPLY.getValue(),
                    OrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    OrderStatus.SHIPPED.getValue());
            //商家拒绝退款退货 -> 买家撤销退货申请 --> 已发货
            addTransition(OrderStatus.RETURN_APPLY_REJECTED.getValue(),
                    OrderEvent.RETURN_APPLY_CANCEL.toOrderOperation(),
                    OrderStatus.SHIPPED.getValue());
            // 商家同意退款退货 -->买家退货 -> 买家已退货
            addTransition(OrderStatus.RETURN_APPLY_AGREED.getValue(),
                    OrderEvent.RETURN.toOrderOperation(),
                    OrderStatus.RETURN.getValue());

            // 买家已退货 -->商家确认货物抵达 -> 商家已确认收退货
            addTransition(OrderStatus.RETURN.getValue(),
                    OrderEvent.RETURN_CONFIRM.toOrderOperation(),
                    OrderStatus.RETURN_CONFIRMED.getValue());

            // 买家已退货 -->商家拒绝货物抵达 -> 商家拒绝退货
            addTransition(OrderStatus.RETURN.getValue(),
                    OrderEvent.RETURN_REJECT.toOrderOperation(),
                    OrderStatus.RETURN_REJECTED.getValue());

            //商家确认退货 -> 退款 --> 商家已退款
            addTransition(OrderStatus.RETURN_CONFIRMED.getValue(),
                    OrderEvent.REFUND.toOrderOperation(),
                    OrderStatus.REFUND.getValue());
        }
    };
}
