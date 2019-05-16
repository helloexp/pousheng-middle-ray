package com.pousheng.middle.search.refund;

import com.google.common.collect.Lists;
import com.pousheng.middle.item.PsCriteriasBuilder;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import io.terminus.common.model.PageInfo;
import io.terminus.search.api.query.Range;
import io.terminus.search.api.query.Sort;
import io.terminus.search.api.query.Term;
import io.terminus.search.api.query.Terms;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-30 17:25<br/>
 */
@Component
public class RefundSearchArgsBuilder {
    public CriteriasWithShould build(MiddleRefundCriteria criteria) {
        PsCriteriasBuilder builder = new PsCriteriasBuilder();
        // 精确查询
        builder.withTerm(buildTerm(criteria));
        // 精确批量查询
        builder.withTerms(buildTerms(criteria));
        // 返回查询
        builder.withRanges(buildRange(criteria));
        // 排除查询
        builder.setNotTerm(buildNotTerm(criteria));
        // 排序
        builder.withSorts(buildSort(criteria));

        PageInfo page = new PageInfo(criteria.getPageNo(), criteria.getPageSize());
        builder.withPageInfo(page.getOffset(), page.getLimit());

        CriteriasWithShould c = new CriteriasWithShould(builder);
        builder.build(c);
        return c;
    }

    /**
     * 排除条件
     */
    private List<Term> buildNotTerm(MiddleRefundCriteria criteria) {
        List<Term> term = org.assertj.core.util.Lists.newArrayList();
        if (criteria.getExcludeRefundType() != null) {
            term.add(new Term("refundType", criteria.getExcludeRefundType()));
        }
        return term;
    }

    /**
     * 排序
     */
    private List<Sort> buildSort(MiddleRefundCriteria criteria) {
        return Collections.singletonList(new Sort("id", "desc"));
    }

    /**
     * 时间范围查询
     */
    private List<Range> buildRange(MiddleRefundCriteria criteria) {
        List<Range> range = org.assertj.core.util.Lists.newArrayList();
        if (criteria.getStartAt() != null || criteria.getEndAt() != null) {
            range.add(new Range("updatedAt", timeString(criteria.getStartAt()), timeString(criteria.getEndAt())));
        }
        if (criteria.getRefundStartAt() != null || criteria.getRefundEndAt() != null) {
            range.add(new Range("refundAt", timeString(criteria.getRefundStartAt()), timeString(criteria.getRefundEndAt())));
        }
        if (criteria.getHkReturnDoneAtStart() != null || criteria.getHkReturnDoneAtEnd() != null) {
            range.add(new Range("hkReturnDoneAt", timeString(criteria.getHkReturnDoneAtStart()), timeString(criteria.getHkReturnDoneAtEnd())));
        }
        return range;
    }

    /**
     * 多值匹配
     */
    private List<Terms> buildTerms(MiddleRefundCriteria criteria) {
        List<Terms> terms = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(criteria.getIds())) {
            terms.add(new Terms("id", criteria.getIds()));
        }
        if (!CollectionUtils.isEmpty(criteria.getShopIds())) {
            terms.add(new Terms("shopId", criteria.getShopIds()));
        }
        if (!CollectionUtils.isEmpty(criteria.getStatus())) {
            terms.add(new Terms("status", criteria.getStatus()));
        }
        return terms;
    }

    /**
     * 精确匹配查询
     */
    private List<Term> buildTerm(MiddleRefundCriteria criteria) {
        List<Term> term = Lists.newArrayList();
        if (StringUtils.hasText(criteria.getShipmentSerialNo())) {
            term.add(new Term("shipmentSerialNo", criteria.getShipmentSerialNo()));
        }
        if (StringUtils.hasText(criteria.getRefundCode())) {
            term.add(new Term("refundCode", criteria.getRefundCode()));
        }
        if (StringUtils.hasText(criteria.getBuyerName())) {
            term.add(new Term("buyerName", criteria.getBuyerName()));
        }
        if (StringUtils.hasText(criteria.getStatusStr())) {
            term.add(new Term("status", criteria.getStatusStr()));
        }
        if (StringUtils.hasText(criteria.getReleOrderCode())) {
            term.add(new Term("releOrderCode", criteria.getReleOrderCode()));
        }
        if (criteria.getShopId() != null) {
            term.add(new Term("shopId", criteria.getShopId()));
        }
        if (criteria.getRefundType() != null) {
            term.add(new Term("refundType", criteria.getRefundType()));
        }
        if (criteria.getCompleteReturn() != null) {
            term.add(new Term("completeReturn", criteria.getCompleteReturn()));
        }
        return term;
    }

    private String timeString(Date input) {
        if (input == null) {
            return null;
        }
        return new DateTime(input).toString();
    }
}
