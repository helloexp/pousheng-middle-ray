package com.pousheng.middle.consume.index.processor.core;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 10:23<br/>
 */
@Slf4j
public class IDEventProcessor {
    private final String indexName;
    private final Function<List<String>, Long> idExtract;
    private final Consumer<Set<Long>> batchExec;

    public IDEventProcessor(String indexName, Function<List<String>, Long> idExtract, Consumer<Set<Long>> batchExec) {
        this.indexName = indexName;
        this.idExtract = idExtract;
        this.batchExec = batchExec;
    }

    public void process(IndexEvent event) {
        this.process(indexName, event, idExtract, batchExec);
    }

    public void process(String indexName, IndexEvent event, Function<List<String>, Long> idExtract, Consumer<Set<Long>> batchExec) {
        Set<Long> ids = Sets.newLinkedHashSet();
        log.info("about to index {} {}", event.getData().size(), indexName);

        List<List<String>> data = event.getData();
        for (List<String> row : data) {
            if (CollectionUtils.isEmpty(row)) {
                continue;
            }

            Long id = idExtract.apply(row);
            if (id == null) {
                log.error("failed to extract id {} to long, skip indexing this record", row);
                continue;
            }
            ids.add(id);
        }

        if (ids.size() > 0) {
            batchExec.accept(ids);
        }
    }
}
