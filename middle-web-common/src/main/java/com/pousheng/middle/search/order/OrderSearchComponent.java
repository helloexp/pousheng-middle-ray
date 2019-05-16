package com.pousheng.middle.search.order;

import com.pousheng.middle.item.PsCriteriasBuilder;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.search.SearchedID;
import io.terminus.common.model.PageInfo;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.Pagination;
import io.terminus.search.api.query.Range;
import io.terminus.search.api.query.Sort;
import io.terminus.search.api.query.Term;
import io.terminus.search.api.query.Terms;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-20 19:31<br/>
 */
@Slf4j
@Component
public class OrderSearchComponent {
    @Value("${order.search.indexName:orders}")
    private String indexName;
    @Value("${order.search.indexType:order}")
    private String indexType;
    @Value("${search.template:ps_search.mustache}")
    private String searchTemplate;

    // @Autowired
    // private Mustacher mustacher;

    private final Searcher searcher;

    public OrderSearchComponent(Searcher searcher) {
        this.searcher = searcher;
    }

    public Pagination<SearchedID> search(MiddleOrderCriteria criteria) {
        PsCriteriasBuilder builder = new PsCriteriasBuilder();
        List<Term> term = buildTerm(criteria);
        List<Terms> terms = buildTerms(criteria);
        if (!CollectionUtils.isEmpty(criteria.getStatus()) && criteria.getStatus().contains(99)) {
            terms.add(new Terms("status", Lists.newArrayList(4, 5)));
            term.add(new Term("ecpOrderStatus", 1));
        }
        // 精确查询
        builder.withTerm(term);
        // 精确批量查询
        builder.withTerms(terms);
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

        // debug
        // Mustache m = mustacher.forPath("ps_search.mustache");
        // try (StringWriter sw = new StringWriter()) {
        //     m.execute(sw, c).flush();
        //     log.info(sw.toString());
        // } catch (Exception e) {
        //     log.error(Throwables.getStackTraceAsString(e));
        // }

        return searcher.search(indexName, indexType, searchTemplate, c, SearchedID.class);
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
        Range r = new Range("createdAt",
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
        if (!CollectionUtils.isEmpty(criteria.getStatus()) && criteria.getStatus().contains(99)) {
            terms.add(new Terms("status", Lists.newArrayList(4, 5)));
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
        if (StringUtils.hasText(criteria.getStatusStr())) {
            term.add(new Term("status", criteria.getStatusStr()));
        }
        if (criteria.getHandleStatus() != null) {
            term.add(new Term("handleStatus", criteria.getHandleStatus()));
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
