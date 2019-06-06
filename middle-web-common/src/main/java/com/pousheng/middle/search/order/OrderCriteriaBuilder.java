package com.pousheng.middle.search.order;

import com.pousheng.middle.item.PsCriteriasBuilder;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import io.terminus.common.model.PageInfo;
import io.terminus.search.api.query.Range;
import io.terminus.search.api.query.Sort;
import io.terminus.search.api.query.Term;
import io.terminus.search.api.query.Terms;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-06 17:01<br/>
 */
@Component
public class OrderCriteriaBuilder {
    private static String ECP_SYNC_FAIL = "99";

    public CriteriasWithShould build(MiddleOrderCriteria criteria) {
        PsCriteriasBuilder builder = new PsCriteriasBuilder();
        // 精确查询
        builder.withTerm(buildTerm(criteria));
        // 精确批量查询
        builder.withTerms(buildTerms(criteria));
        // 模糊查询（非搜索）
        builder.setWildcard(buildWildcard(criteria));
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

    private List<Term> buildWildcard(MiddleOrderCriteria criteria) {
        List<Term> wildcard = Lists.newArrayList();
        if (StringUtils.hasText(criteria.getBuyerNoteLike())) {
            wildcard.add(new Term("buyerNote.keyword", "*" + criteria.getBuyerNoteLike() + "*"));
        }
        return wildcard;
    }

    private List<Term> buildNotTerm(MiddleOrderCriteria criteria) {
        List<Term> term = Lists.newArrayList();
        if (StringUtils.hasText(criteria.getExcludeOutFrom())) {
            term.add(new Term("outFrom", criteria.getExcludeOutFrom()));
        }
        return term;
    }

    /**
     * 排序
     */
    private List<Sort> buildSort(MiddleOrderCriteria criteria) {
        return Collections.singletonList(new Sort("createdAt", "desc"));
    }

    /**
     * 时间范围查询
     */
    private List<Range> buildRange(MiddleOrderCriteria criteria) {
        List<Range> range = Lists.newArrayList();
        Range r = new Range("outCreatedAt",
                timeString(criteria.getOutCreatedStartAt()),
                timeString(criteria.getOutCreatedEndAt()));
        r.setLast(true);

        range.add(r);
        return range;
    }

    /**
     * 多值匹配
     */
    private List<Terms> buildTerms(MiddleOrderCriteria criteria) {
        List<Terms> terms = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(criteria.getShopIds())) {
            terms.add(new Terms("shopId", criteria.getShopIds()));
        }
        // 同步电商失败
        if (ECP_SYNC_FAIL.equals(criteria.getStatusStr())) {
            terms.add(new Terms("status", Lists.newArrayList(4, 5)));
        }
        if (!CollectionUtils.isEmpty(criteria.getStatus())) {
            terms.add(new Terms("status", criteria.getStatus()));
        }
        return terms;
    }

    /**
     * 精确匹配查询
     */
    private List<Term> buildTerm(MiddleOrderCriteria criteria) {
        List<Term> term = Lists.newArrayList();
        // 订单、外部订单
        if (StringUtils.hasText(criteria.getOrderCode())) {
            term.add(new Term("orderCode", criteria.getOrderCode()));
        }
        if (StringUtils.hasText(criteria.getOutId())) {
            term.add(new Term("outId", criteria.getOutId()));
        }
        // 状态
        if (StringUtils.hasText(criteria.getStatusStr()) && !ECP_SYNC_FAIL.equals(criteria.getStatusStr())) {
            term.add(new Term("status", criteria.getStatusStr()));
        }
        if (criteria.getHandleStatus() != null) {
            term.add(new Term("handleStatus", criteria.getHandleStatus()));
        }
        // 同步电商失败
        if (ECP_SYNC_FAIL.equals(criteria.getStatusStr())) {
            term.add(new Term("ecpOrderStatus", "-1"));
        }
        // 公司 ID、是否赠品
        if (criteria.getCompanyId() != null) {
            term.add(new Term("companyId", criteria.getCompanyId()));
        }
        // 店铺
        if (criteria.getShopId() != null) {
            term.add(new Term("shopId", criteria.getShopId()));
        }
        if (StringUtils.hasText(criteria.getShopName())) {
            term.add(new Term("shopName", criteria.getShopName()));
        }
        // 买家
        if (StringUtils.hasText(criteria.getBuyerName())) {
            term.add(new Term("buyerName", criteria.getBuyerName()));
        }
        if (StringUtils.hasText(criteria.getMobile())) {
            term.add(new Term("buyerPhone", criteria.getMobile()));
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
