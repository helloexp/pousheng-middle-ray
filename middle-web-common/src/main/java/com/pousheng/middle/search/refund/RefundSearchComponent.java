package com.pousheng.middle.search.refund;

import com.google.common.collect.Maps;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.service.MiddleRefundReadService;
import com.pousheng.middle.search.SearchedID;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 15:04<br/>
 */
@Slf4j
@Component
public class RefundSearchComponent {
    @Value("${order.search.indexName:refunds}")
    private String indexName;
    @Value("${order.search.indexType:refund}")
    private String indexType;
    @Value("${search.template:ps_search.mustache}")
    private String searchTemplate;

    private final Searcher searcher;
    private final RefundReadService refundReadService;
    private final MiddleRefundReadService middleRefundReadService;
    private final RefundSearchArgsBuilder refundSearchArgsBuilder;

    public RefundSearchComponent(Searcher searcher,
                                 RefundReadService refundReadService,
                                 MiddleRefundReadService middleRefundReadService,
                                 RefundSearchArgsBuilder refundSearchArgsBuilder) {
        this.searcher = searcher;
        this.refundReadService = refundReadService;
        this.middleRefundReadService = middleRefundReadService;
        this.refundSearchArgsBuilder = refundSearchArgsBuilder;
    }

    public Paging<RefundPaging> search(MiddleRefundCriteria criteria) {
        CriteriasWithShould c = refundSearchArgsBuilder.build(criteria);
        Pagination<SearchedID> result = searcher.search(indexName, indexType, searchTemplate, c, SearchedID.class);
        return buildSearchReasult(result);
    }

    private Paging<RefundPaging> buildSearchReasult(Pagination<SearchedID> searched) {
        if (searched.getData().size() == 0 || searched.getTotal() == 0) {
            return Paging.empty();
        }
        List<Long> ids = searched.getData().stream().map(SearchedID::getId).collect(Collectors.toList());
        Response<List<Refund>> r = refundReadService.findByIds(ids);
        if (!r.isSuccess()) {
            log.error("failed to find refunds by ids: {}, cause: {}", ids, r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Refund> refunds = r.getResult();
        Response<Map<Long, OrderRefund>> groupByRefundIdMapRes = groupOrderRerundByRefundId(refunds);
        if (!groupByRefundIdMapRes.isSuccess()) {
            throw new JsonResponseException(groupByRefundIdMapRes.getError());
        }

        Map<Long, OrderRefund> groupByRefundIdMap = groupByRefundIdMapRes.getResult();
        Paging<RefundPaging> paging = new Paging<>();
        paging.setTotal(searched.getTotal());

        Map<Long, RefundPaging> idTORefund = Maps.newHashMap();
        for (Refund refund : refunds) {
            RefundPaging rp = new RefundPaging();
            rp.setRefund(refund);
            rp.setOrderRefund(groupByRefundIdMap.get(refund.getId()));
            Set<OrderOperation> operations = MiddleFlowBook.afterSalesFlow.availableOperations(refund.getStatus());
            rp.setOperations(operations);
            idTORefund.put(refund.getId(), rp);
        }

        List<RefundPaging> refundLists = ids.stream().map(idTORefund::get).filter(Objects::nonNull).collect(Collectors.toList());
        paging.setData(refundLists);
        return paging;
    }

    private Response<Map<Long, OrderRefund>> groupOrderRerundByRefundId(List<Refund> refunds) {

        if (CollectionUtils.isEmpty(refunds)) {
            return Response.ok(Maps.newHashMap());
        }
        List<Long> refundIds = refunds.stream().map(Refund::getId).collect(Collectors.toList());
        Response<List<OrderRefund>> listRes = middleRefundReadService.findOrderRefundByRefundIds(refundIds);
        if (!listRes.isSuccess()) {
            log.error("find order refund by refund ids:{} fail,error:{}", refundIds, listRes.getError());
            return Response.fail(listRes.getError());
        }

        Map<Long, OrderRefund> groupByRefundIdMap = listRes.getResult().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(OrderRefund::getRefundId, Function.identity()));
        return Response.ok(groupByRefundIdMap);
    }
}
