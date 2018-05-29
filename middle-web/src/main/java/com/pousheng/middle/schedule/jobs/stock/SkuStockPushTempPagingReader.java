package com.pousheng.middle.schedule.jobs.stock;

import com.google.common.collect.Maps;
import com.pousheng.middle.schedule.jobs.MyBatisPagingItemReader;
import io.terminus.common.model.PageInfo;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.springframework.util.StringUtils.hasText;

/**
 * Description: 库存任务同步任务
 * Author: xiao
 * Date: 2018/05/29
 */
@Slf4j
public class SkuStockPushTempPagingReader extends MyBatisPagingItemReader<String> {

    @Setter
    @Value("#{jobParameters[start]}")
    private Long idStart;

    @Setter
    @Value("#{jobParameters[end]}")
    private Long idEnd;

    @Override
    protected synchronized void doReadPage() {
        if (log.isDebugEnabled()) {
            log.debug("{} Process start {}, end {}", Thread.currentThread().getName(), idStart, idEnd);
        }

        PageInfo pageInfo = new PageInfo(getPage() + 1, getPageSize());
        Map<String, Object> params = Maps.newHashMap();
        params.put("offset", pageInfo.getOffset());
        params.put("limit", pageInfo.getLimit());
        params.put("idStart", idStart);
        params.put("idEnd", idEnd);

        List<String> skuCodes = getSqlSessionTemplate().selectList("SkuStockPushTempId.paging", params);
        log.info("Load data size {}", skuCodes.size());

        if (results == null) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }

        for (String skuCode : skuCodes) {
            if (hasText(skuCode)) {
                results.add(skuCode);
            }
        }
    }
}
