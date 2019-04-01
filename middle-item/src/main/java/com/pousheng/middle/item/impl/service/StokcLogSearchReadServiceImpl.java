package com.pousheng.middle.item.impl.service;

import com.pousheng.middle.item.SearchStockLogProperties;
import com.pousheng.middle.item.StockLogQueryBuilder;
import com.pousheng.middle.item.service.StockLogSearchReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.search.api.query.Criterias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/8/15
 */
@Service
@Slf4j
@RpcProvider
public class StokcLogSearchReadServiceImpl implements StockLogSearchReadService {

    @Autowired
    private Searcher searcher;

    @Autowired
    private StockLogQueryBuilder stockLogQueryBuilder;

    @Autowired
    private SearchStockLogProperties searchStockLogProperties;

    @Override
    public <T> Response<? extends WithAggregations<T>> searchWithAggs(Integer pageNo, Integer pageSize,
                                                                          String templateName, Map<String, String> params,
                                                                          Class<T> clazz) {
        //构建搜索条件并进行搜索
        Criterias criterias = stockLogQueryBuilder.makeCriterias(pageNo, pageSize, params);

        WithAggregations<T> withAggs = searcher.searchWithAggs(
                searchStockLogProperties.getIndexName(),
                searchStockLogProperties.getIndexType(),
                templateName,
                criterias,
                clazz);

        return Response.ok(withAggs);
    }
}
