/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.item.search;

import com.pousheng.middle.item.dto.IndexedStockLog;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.item.service.StockLogSearchReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/8/20
 */
@Api(description = "日志搜索API基于ES")
@RestController
@Slf4j
public class StockLogSearches {

    @RpcConsumer
    private StockLogSearchReadService stockLogSearchReadService;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    /**
     * 搜索库存变更日志，仅包含恒康推中台及交易变
     *
     * @param pageNo   起始页码
     * @param pageSize 每页记录条数
     * @param params
     * @return 搜索结果
     */
    @ApiOperation("搜索库存日志")
    @RequestMapping(value = "/api/middle/stock/log/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<? extends WithAggregations<IndexedStockLog>> searchItemWithAggs(@RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                                                    @RequestParam(required = false, defaultValue = "20") Integer pageSize,
                                                                                    @RequestParam Map<String, String> params) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-SKU-TEMPLATE-SEARCH-START param: pageNo [{}] pageSize [{}] params [{}]", pageNo, pageSize, JsonMapper.nonEmptyMapper().toJson(params));
        }
        String templateName = "ps_search.mustache";
        Response<? extends WithAggregations<IndexedStockLog>> response = stockLogSearchReadService.searchWithAggs(pageNo, pageSize, templateName, params, IndexedStockLog.class);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-SKU-TEMPLATE-SEARCH-START param: pageNo [{}] pageSize [{}] params [{}] ,resp: [{}]", pageNo, pageSize, JsonMapper.nonEmptyMapper().toJson(params), JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response;
    }


}
