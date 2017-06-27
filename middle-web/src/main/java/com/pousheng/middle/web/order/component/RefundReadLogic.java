package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.service.MiddleRefundReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class RefundReadLogic {

    @Autowired
    private MiddleRefundReadService middleRefundReadService;
    @RpcConsumer
    private RefundReadService refundReadService;

    @Autowired
    private MiddleOrderFlowPicker flowPicker;


    public Response<Paging<RefundPaging>> refundPaging(RefundCriteria criteria) {
        Response<Paging<Refund>> refundsR = middleRefundReadService.paging(criteria);
        if (!refundsR.isSuccess()) {
            log.error("paging refund by criteria:{} fail,error:{}",criteria,refundsR.getError());
            return Response.fail(refundsR.getError());
        }

        Paging<Refund> refundPaging = refundsR.getResult();
        List<Refund> refunds = refundPaging.getData();
        Response<Map<Long,OrderRefund>> groupByRefundIdMapRes = groupOrderRerundByRefundId(refunds);
        if(!groupByRefundIdMapRes.isSuccess()){
            return Response.fail(groupByRefundIdMapRes.getError());
        }

        Map<Long,OrderRefund>  groupByRefundIdMap = groupByRefundIdMapRes.getResult();
        Flow flow = flowPicker.pickAfterSales();
        Paging<RefundPaging> paging = new Paging<>();
        paging.setTotal(refundPaging.getTotal());
        List<RefundPaging> refundLists = Lists.newArrayListWithCapacity(refunds.size());

        for (Refund refund : refunds) {
            RefundPaging rp  = new RefundPaging();
            rp.setRefund(refund);
            rp.setOrderRefund(groupByRefundIdMap.get(refund.getId()));
            Set<OrderOperation> operations = flow.availableOperations(refund.getStatus());
            rp.setOperations(operations);
            refundLists.add(rp);
        }
        paging.setData(refundLists);
        return Response.ok(paging);
    }

    public Response<Map<Long,OrderRefund>> groupOrderRerundByRefundId(List<Refund> refunds){

        if(CollectionUtils.isEmpty(refunds)){
            return Response.ok(Maps.newHashMap());
        }
        List<Long> refundIds = Lists.transform(refunds, new Function<Refund, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Refund refund) {
                return refund.getId();
            }
        });
        Response<List<OrderRefund>> listRes = middleRefundReadService.findOrderRefundByRefundIds(refundIds);
        if(!listRes.isSuccess()){
            log.error("find order refund by refund ids:{} fail,error:{}",refundIds,listRes.getError());
            return Response.fail(listRes.getError());
        }
        List<OrderRefund> orderRefunds = listRes.getResult();
        Map<Long,OrderRefund> groupByRefundIdMap = orderRefunds.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(OrderRefund::getRefundId, it -> it));
        return Response.ok(groupByRefundIdMap);
    }

    public Refund findRefundById(Long refunId){
        Response<Refund> refundRes = refundReadService.findById(refunId);
        if(!refundRes.isSuccess()){
            log.error("find refund by id:{} fail,error:{}",refunId,refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }

    public OrderRefund findOrderRefundByRefundId(Long refundId){
        Response<List<OrderRefund>> listRes = refundReadService.findOrderIdsByRefundId(refundId);
        if(!listRes.isSuccess()){
            log.error("find order refund by refund id:{} fail,error:{}",refundId,listRes.getError());
            throw new JsonResponseException(listRes.getError());
        }

        List<OrderRefund> orderRefunds = listRes.getResult();
        if(CollectionUtils.isEmpty(orderRefunds)){
            log.error("not find order refund by refund id:{}",refundId);
            throw new JsonResponseException("order.refund.not.exist");
        }

        return orderRefunds.get(0);

    }
}
