package com.pousheng.middle.consume.index.processor.impl.order;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.pousheng.middle.consume.index.configuration.OrderSearchProperties;
import com.pousheng.middle.consume.index.processor.core.IDEventProcessor;
import com.pousheng.middle.consume.index.processor.core.IndexEvent;
import com.pousheng.middle.consume.index.processor.core.IndexEventProcessor;
import com.pousheng.middle.consume.index.processor.core.Processor;
import com.pousheng.middle.consume.index.processor.impl.order.builder.OrderIndexManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 14:05<br/>
 */
@Slf4j
@Component
@Processor("parana_shop_orders")
public class ShopOrderProcessor implements IndexEventProcessor {
    private final IDEventProcessor idEventProcessor;

    public ShopOrderProcessor(OrderIndexManager orderIndexerManager, OrderSearchProperties orderSearchProperties) {
        idEventProcessor = new IDEventProcessor(orderSearchProperties.getIndexName(),
                row -> Longs.tryParse(row.get(0)),
                ids -> {
                    if (ids.size() > 10) {
                        List<List<Long>> slices = Lists.partition(new ArrayList<>(ids), 100);
                        slices.forEach(orderIndexerManager::bulkIndex);
                    } else {
                        ids.forEach(orderIndexerManager::index);
                    }
                });
    }

    @Override
    public void process(IndexEvent event) {
        Stopwatch sw = Stopwatch.createStarted();
        idEventProcessor.process(event);
        if (event.getData().size() > 10) {
            log.info("total {} orders indexed, cost: {}", event.getData().size(), sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
