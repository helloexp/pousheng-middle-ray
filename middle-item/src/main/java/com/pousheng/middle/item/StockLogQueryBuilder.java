/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item;

import com.google.common.collect.Lists;
import io.terminus.common.model.PageInfo;
import io.terminus.parana.search.item.impl.DefaultItemQueryBuilder;
import io.terminus.search.api.query.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 搜索查询参数设置
 *
 * @author zxw
 */
@Slf4j
@Component
public class StockLogQueryBuilder extends DefaultItemQueryBuilder {

    @Override
    public List<Term> buildTerm(Map<String, String> params) {
        List<Term> termList = Lists.newArrayList();
        // 仓库code
        String type = params.get("type");
        if (StringUtils.hasText(type)) {
            termList.add(new Term("type", type));
        }
        // skuCode
        String skuCode = params.get("skuCode");
        if (StringUtils.hasText(skuCode)) {
            termList.add(new Term("skuCode", skuCode));
        }
        // 货号
        String materialId = params.get("materialId");
        if (StringUtils.hasText(materialId)) {
            termList.add(new Term("materialId", materialId));
        }
        // 发货单号
        String shipmentId = params.get("shipmentId");
        if (StringUtils.hasText(shipmentId)) {
            termList.add(new Term("shipmentId", shipmentId));
        }
        // 仓库编码
        String warehouseCode = params.get("warehouseCode");
        if (StringUtils.hasText(warehouseCode)) {
            termList.add(new Term("warehouseCode", warehouseCode));
        }
        // 仓库名称
        String warehouseName = params.get("warehouseName");
        if (StringUtils.hasText(warehouseName)) {
            termList.add(new Term("warehouseName", warehouseName));
        }
        // 状态
        String status = params.get("status");
        if (StringUtils.hasText(status)) {
            termList.add(new Term("status", status));
        }
        // 操作
        String operate = params.get("operate");
        if (StringUtils.hasText(operate)) {
            termList.add(new Term("operate", operate));
        }

        return termList;
    }

    @Override
    public Criterias makeCriterias(Integer pageNo, Integer size, Map<String, String> params) {
        PsCriteriasBuilder criteriasBuilder = new PsCriteriasBuilder();
        PageInfo pageInfo = new PageInfo(pageNo, size);
        criteriasBuilder.withPageInfo(pageInfo.getOffset(), pageInfo.getLimit());
        Keyword keyword = this.buildKeyword(params);
        criteriasBuilder.withKeyword(keyword);
        criteriasBuilder.withSorts(Lists.newArrayList(new Sort("createdAt", "desc")));
        List<Term> termList = this.buildTerm(params);
        criteriasBuilder.withTerm(termList);
        List<Terms> termsList = this.buildTerms(params);
        criteriasBuilder.withTerms(termsList);
        List<Range> ranges = this.buildRanges(params);
        criteriasBuilder.withRanges(ranges);
        return criteriasBuilder.build();

    }

    @Override
    public List<Range> buildRanges(Map<String, String> params) {
        List<Range> ranges = Lists.newArrayList();
        String after = params.get("startAt");
        String before = params.get("endAt");
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        if (StringUtils.hasText(after) || StringUtils.hasText(before)) {
            Range dateRange = new Range("createdAt", StringUtils.isEmpty(after) ? null
                    : DateTime.parse(after + " 00:00:00", format).toDate().getTime(),
                    StringUtils.isEmpty(before) ? null
                            : DateTime.parse(before + " 23:59:59", format).toDate().getTime());
            ranges.add(dateRange);
        }
        return ranges;
    }

}
