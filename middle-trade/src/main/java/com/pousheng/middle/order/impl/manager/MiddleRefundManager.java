package com.pousheng.middle.order.impl.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.parana.order.impl.dao.OrderRefundDao;
import io.terminus.parana.order.impl.dao.RefundDao;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/28
 */
@Component
public class MiddleRefundManager {

    @Autowired
    private RefundDao refundDao;

    @Autowired
    private OrderRefundDao orderRefundDao;


    /**
     * 创建退款单, 同时创建退款单和对应(子)订单的关联关系
     *
     * @param refund  退款单
     * @param orderRefunds  (子)订单和退款单的关联关系
     * @return   新创建的退款单id
     */
    @Transactional
    public Long create(Refund refund, List<OrderRefund> orderRefunds) {
        boolean success = refundDao.create(refund);
        if (!success) {
            throw new ServiceException("refund.create.fail");
        }
        Long refundId = refund.getId();

        for (OrderRefund orderRefund : orderRefunds) {
            orderRefund.setRefundId(refundId);
        }
        orderRefundDao.creates(orderRefunds);
        return refundId;
    }
}
