package com.pousheng.middle.consume.index.mock;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 10:06<br/>
 */
public class RefundReadServiceMock implements RefundReadService {
    @Override
    public Response<Refund> findById(Long aLong) {
        return null;
    }

    @Override
    public Response<Refund> findByRefundCode(String s) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByIds(List<Long> list) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByOrderIdAndOrderLevel(Long aLong, OrderLevel orderLevel) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByOrderCodeAndOrderLevel(String s, OrderLevel orderLevel) {
        return null;
    }

    @Override
    public Response<List<OrderRefund>> findOrderIdsByRefundId(Long aLong) {
        return null;
    }

    @Override
    public Response<List<OrderRefund>> findOrderRefundsByRefundCodes(String s) {
        return null;
    }

    @Override
    public Response<Refund> findByOutId(String s) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByTradeNo(String s) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByTradeNoAndCreatedAt(String s, Date date) {
        return null;
    }

    @Override
    public Response<Paging<Refund>> findRefundBy(Integer integer, Integer integer1, RefundCriteria refundCriteria) {
        return null;
    }

    @Override
    public Response<Refund> findByAfterSaleId(Long aLong) {
        return null;
    }

    @Override
    public Response<List<Refund>> findByShipInfo(String s) {
        return null;
    }
}
