package com.pousheng.middle.schedule.jobs.stock;

import com.pousheng.middle.open.StockPusher;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description: 第三方库存推送全量任务
 * Author: xiao
 * Date: 2018/05/29
 */
@Slf4j
public class SkuStockPushTempProcessor implements ItemProcessor<String, String> {

    private final StockPusher stockPusher;

    @Autowired
    public SkuStockPushTempProcessor(StockPusher stockPusher) {
        this.stockPusher = stockPusher;
    }


    @Override
    public String process(String item) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Process skuCode {}", item);
        }
        log.info("Process skuCode {}", item);

        stockPusher.submit(Lists.newArrayList(item));
        return item;
    }
}
