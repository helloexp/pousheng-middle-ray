package com.pousheng.middle.consume.index.processor.impl.refund;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.pousheng.middle.consume.index.configuration.RefundSearchProperties;
import com.pousheng.middle.consume.index.processor.core.IDEventProcessor;
import com.pousheng.middle.consume.index.processor.core.IndexEvent;
import com.pousheng.middle.consume.index.processor.core.IndexEventProcessor;
import com.pousheng.middle.consume.index.processor.core.Processor;
import com.pousheng.middle.consume.index.processor.impl.refund.builder.RefundIndexManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-21 20:48<br/>
 */
@Slf4j
@Component
@Processor("parana_refunds")
public class RefundProcessor implements IndexEventProcessor {
    private final IDEventProcessor idEventProcessor;

    public RefundProcessor(RefundIndexManager refundIndexManager, RefundSearchProperties refundSearchProperties) {
        idEventProcessor = new IDEventProcessor(refundSearchProperties.getIndexName(),
                row -> Longs.tryParse(row.get(0)),
                ids -> {
                    if (ids.size() > 100) {
                        List<List<Long>> slices = Lists.partition(new ArrayList<>(ids), 500);
                        slices.forEach(refundIndexManager::bulkIndex);
                    } else {
                        ids.forEach(refundIndexManager::index);
                    }
                });
    }

    @Override
    public void process(IndexEvent event) {
        Stopwatch sw = Stopwatch.createStarted();
        idEventProcessor.process(event);
        log.info("total {} refund indexed, cost: {}", event.getData().size(), sw.elapsed(TimeUnit.MILLISECONDS));
    }
}
