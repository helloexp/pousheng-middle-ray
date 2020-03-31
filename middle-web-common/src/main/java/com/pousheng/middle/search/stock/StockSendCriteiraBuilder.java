package com.pousheng.middle.search.stock;

import com.google.common.collect.Lists;
import com.pousheng.middle.item.PsCriteriasBuilder;
import com.pousheng.middle.item.service.CriteriasWithShould;
import com.pousheng.middle.search.dto.StockSendCriteria;
import io.terminus.common.model.PageInfo;
import io.terminus.search.api.query.Sort;
import io.terminus.search.api.query.Term;
import io.terminus.search.api.query.Terms;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-18 15:50<br/>
 */
@Component
public class StockSendCriteiraBuilder {
    public CriteriasWithShould build(StockSendCriteria criteria) {
        PsCriteriasBuilder builder = new PsCriteriasBuilder();
        // 精确查询
        builder.withTerm(buildTerm(criteria));
        // 精确批量查询
        builder.withTerms(buildTerms(criteria));

        // builder.setWildcard(buildWildcard(criteria));
        // 返回查询
        // builder.withRanges(buildRange(criteria));
        // 排除查询
        // builder.setNotTerm(buildNotTerm(criteria));
        // 排序
        builder.withSorts(buildSort(criteria));

        PageInfo page = new PageInfo(criteria.getPageNo(), criteria.getPageSize());
        builder.withPageInfo(page.getOffset(), page.getLimit());

        CriteriasWithShould c = new CriteriasWithShould(builder);
        builder.build(c);
        return c;
    }

    private List<Term> buildTerm(StockSendCriteria criteria) {
        List<Term> term = Lists.newArrayList();
        if (criteria.getZoneId() != null) {
            term.add(new Term("zoneId", criteria.getZoneId()));
        }
        if (criteria.getShopId() != null) {
            term.add(new Term("shopId", criteria.getShopId()));
        }
        if (criteria.getShopType() != null) {
            term.add(new Term("shopType", criteria.getShopType()));
        }
        if (StringUtils.hasText(criteria.getShopName())) {
            term.add(new Term("shopName", criteria.getShopName()));
        }
        if (StringUtils.hasText(criteria.getWarehouseCompanyCode())) {
            term.add(new Term("warehouseCompanyCode", criteria.getWarehouseCompanyCode()));
        }
        if (StringUtils.hasText(criteria.getShopOutCode())) {
            term.add(new Term("shopOutCode", criteria.getShopOutCode()));
        }
        if (StringUtils.hasText(criteria.getWarehouseOutCode())) {
            term.add(new Term("warehouseOutCode", criteria.getWarehouseOutCode()));
        }
        if (StringUtils.hasText(criteria.getWarehouseName())) {
            term.add(new Term("warehouseName", criteria.getWarehouseName()));
        }
        return term;
    }

    private List<Terms> buildTerms(StockSendCriteria criteria) {
        List<Terms> terms = Lists.newArrayList();

        if (!CollectionUtils.isEmpty(criteria.getZoneIds())) {
            terms.add(new Terms("zoneId", criteria.getZoneIds()));
        }
        if (!CollectionUtils.isEmpty(criteria.getShopIds())) {
            terms.add(new Terms("shopId", criteria.getShopIds()));
        }

        return terms;
    }

    private List<Sort> buildSort(StockSendCriteria criteria) {
        List<Sort> sort = Lists.newArrayList();
        sort.add(new Sort("shopId", "asc"));
        sort.add(new Sort("warehouseId", "asc"));
        return sort;
    }


//    private List<Term> buildWildcard(StockSendCriteria criteria) {
//        List<Term> wildcard = Lists.newArrayList();
//        if (StringUtils.hasText(criteria.getWarehouseName())) {
//            wildcard.add(new Term("warehouseName", "*" + criteria.getWarehouseName() + "*"));
//        }
//        return wildcard;
//    }
}
