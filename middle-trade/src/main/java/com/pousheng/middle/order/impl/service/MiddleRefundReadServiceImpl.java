package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.enums.MiddleRefundFlagEnum;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
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
import org.apache.commons.lang3.StringUtils;
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
    public Response<Paging<Refund>> paging(MiddleRefundCriteria criteria) {
        try {

            PageInfo pageInfo = new PageInfo(criteria.getPageNo(), criteria.getSize());


            //把订单id,订单类型转为退款单id列表
            transformOrderIdAndOrderType(criteria);
            if (StringUtils.isNotEmpty(criteria.getOrderCode()) && CollectionUtils.isEmpty(criteria.getIds())) {

                return Response.ok(Paging.empty());
            }

            handleDate(criteria);
            //状态多个就不能这么判断了；目前状态-99，只有中台有用到
            if(criteria.getStatus()!=null&& criteria.getStatus().contains(MiddleRefundStatus.REFUND_SYNC_ECP_FAIL.getValue())){
                criteria.setStatus(Lists.newArrayList(MiddleRefundStatus.SYNC_ECP_SUCCESS_WAIT_REFUND.getValue()));
                criteria.setRefundFlag(MiddleRefundFlagEnum.REFUND_SYN_THIRD_PLANT.getValue());
            }
            Map<String, Object> params = criteria.toMap();
//            orderRefundDao.paging(pageInfo.getOffset(),pageInfo.getLimit(),params)
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

    @Override
    public Response<List<OrderRefund>> findOrderRefundsByOrderId(Long shopOrderId) {
        try {

            return Response.ok(orderRefundDao.findByOrderIdAndOrderType(shopOrderId,OrderLevel.SHOP));

        }catch (Exception e){
            log.error("find order refunds by orderId ids:{} fail,cause:{}",shopOrderId,Throwables.getStackTraceAsString(e));
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


    private void transformOrderIdAndOrderType(MiddleRefundCriteria refundCriteria) {
        String orderCode = refundCriteria.getOrderCode();
        if (null == orderCode) {
            return;
        }
        Integer orderLevel = MoreObjects.firstNonNull(refundCriteria.getOrderLevel(), OrderLevel.SHOP.getValue());

        List<OrderRefund> orderRefunds = orderRefundDao.findByOrderCodeAndOrderType(orderCode, OrderLevel.fromInt(orderLevel));
        List<Long> refundIs = retrieveRefundIds(orderRefunds);

        refundCriteria.setIds(refundIs);
    }

    private List<Long> retrieveRefundIds(List<OrderRefund> orderRefunds) {
        return Lists.transform(orderRefunds, OrderRefund::getRefundId);
    }

}
