package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.impl.manager.MiddleRefundManager;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/28
 */
@Slf4j
@Service
public class MiddleRefundWriteServiceImpl implements MiddleRefundWriteService{

    @Autowired
    private MiddleRefundManager middleRefundManager;


    @Override
    public Response<Long> create(Refund refund, List<Long> orderIds, OrderLevel orderLevel) {

        try {
            //默认状态为申请退款
            refund.setStatus(MoreObjects.firstNonNull(refund.getStatus(), OrderStatus.REFUND_APPLY.getValue()));
            List<OrderRefund> orderRefunds = Lists.newArrayListWithCapacity(orderIds.size());
            for (Long orderId : orderIds) {
                OrderRefund orderRefund = new OrderRefund();
                orderRefund.setOrderId(orderId);
                orderRefund.setOrderLevel(orderLevel);
                orderRefund.setStatus(refund.getStatus());
                orderRefunds.add(orderRefund);
            }
            Long refundId = middleRefundManager.create(refund, orderRefunds);
            return Response.ok(refundId);
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", refund, Throwables.getStackTraceAsString(e));
            return Response.fail("refund.create.fail");
        }
    }
}
