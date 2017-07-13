package com.pousheng.middle.order.impl.service;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.service.MiddleRefundReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.DateUtil;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.impl.dao.OrderRefundDao;
import io.terminus.parana.order.impl.dao.RefundDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/6/26
 */
@Slf4j
@Service
public class MiddleRefundReadServiceImpl implements MiddleRefundReadService {

    @Autowired
    private RefundDao refundDao;
    @Autowired
    private OrderRefundDao orderRefundDao;

    @Override
    public Response<Paging<Refund>> paging(RefundCriteria criteria) {
        try {

            PageInfo pageInfo = new PageInfo(criteria.getPageNo(), criteria.getSize());


            //把订单id,订单类型转为退款单id列表
            transformOrderIdAndOrderType(criteria);
            if (criteria.getOrderId() != null && CollectionUtils.isEmpty(criteria.getIds())) {
                return Response.ok(Paging.empty());
            }

            handleDate(criteria);
            Map<String, Object> params = criteria.toMap();
            Paging<Refund> refundP = refundDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            if (Objects.equals(refundP.getTotal(), 0L)) {
                return Response.ok(Paging.<Refund>empty());
            }

            return Response.ok(refundP);
        }catch (Exception e){
            log.error("failed to find order refunds by {}, cause:{}",
                    criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("order.refund.find.fail");
        }
    }

    @Override
    public Response<List<OrderRefund>> findOrderRefundByRefundIds(List<Long> refundIds) {
        try {

            return Response.ok(orderRefundDao.findByRefundIds(refundIds));

        }catch (Exception e){
            log.error("find order refund by refund ids:{} fail,cause:{}",refundIds,Throwables.getStackTraceAsString(e));
            return Response.fail("order.refund.find.fail");
        }
    }

    private void handleDate(RefundCriteria refundCriteria) {
        if (refundCriteria.getStartAt() != null) {
            refundCriteria.setStartAt(DateUtil.withTimeAtStartOfDay(refundCriteria.getStartAt()));
        }
        if (refundCriteria.getEndAt() != null) {
            refundCriteria.setEndAt(DateUtil.withTimeAtEndOfDay(refundCriteria.getEndAt()));
        }
    }


    private void transformOrderIdAndOrderType(RefundCriteria refundCriteria) {
        Long orderId = refundCriteria.getOrderId();
        if (null == orderId) {
            return;
        }
        Integer orderLevel = MoreObjects.firstNonNull(refundCriteria.getOrderLevel(), OrderLevel.SHOP.getValue());

        List<OrderRefund> orderRefunds = orderRefundDao.findByOrderIdAndOrderType(orderId, OrderLevel.fromInt(orderLevel));
        List<Long> refundIs = retrieveRefundIds(orderRefunds);

        refundCriteria.setIds(refundIs);
    }

    private List<Long> retrieveRefundIds(List<OrderRefund> orderRefunds) {
        return Lists.transform(orderRefunds, new Function<OrderRefund, Long>() {
            @Override
            public Long apply(OrderRefund input) {
                return input.getRefundId();
            }
        });
    }

}
