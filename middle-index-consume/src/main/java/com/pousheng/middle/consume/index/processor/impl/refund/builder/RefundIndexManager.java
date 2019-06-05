package com.pousheng.middle.consume.index.processor.impl.refund.builder;

import com.google.common.base.Throwables;
import com.pousheng.middle.consume.index.configuration.RefundSearchProperties;
import com.pousheng.middle.consume.index.processor.impl.BulkDocument;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.search.api.IndexExecutor;
import io.terminus.search.api.IndexTaskBuilder;
import io.terminus.search.api.Indexer;
import io.terminus.search.api.model.BulkIndexTask;
import io.terminus.search.api.model.IndexAction;
import io.terminus.search.api.model.IndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-21 20:50<br/>
 */
@Slf4j
@Component
public class RefundIndexManager {
    @RpcConsumer
    private RefundReadService refundReadService;

    private final RefundDocumentBuilder refundDocumentBuilder;
    private final RefundSearchProperties refundSearchProperties;
    private final IndexTaskBuilder indexTaskBuilder;
    private final Indexer indexer;

    public RefundIndexManager(RefundSearchProperties refundSearchProperties, IndexTaskBuilder indexTaskBuilder, IndexExecutor indexExecutor, Indexer indexer) {
        this.refundSearchProperties = refundSearchProperties;
        this.indexTaskBuilder = indexTaskBuilder;
        this.indexer = indexer;
        refundDocumentBuilder = new RefundDocumentBuilder();
    }

    public void index(Long id) {
        try {
            RefundDocument refundDocument = buildDocument(id);
            if (refundDocument == null) {
                log.error("refund not found by id {}", id);
            }
            IndexTask task = indexTaskBuilder.indexName(refundSearchProperties.getIndexName())
                    .indexType(refundSearchProperties.getIndexType())
                    .indexAction(IndexAction.INDEX)
                    .build(id, refundDocument);
            task.run();
        } catch (Exception e) {
            log.error("failed to index refund {}, cause: {}", id, Throwables.getStackTraceAsString(e));
        }
    }

    public void bulkIndex(List<Long> ids) {
        try {
            // 更新
            List<BulkDocument> documents = ids.stream()
                    .map(this::buildDocument)
                    .filter(Objects::nonNull)
                    .map(this::bulkDocument)
                    .collect(Collectors.toList());
            if (documents.isEmpty()) {
                return;
            }

            BulkIndexTask indexTask = new BulkIndexTask(indexer,
                    refundSearchProperties.getIndexName(),
                    refundSearchProperties.getIndexType(),
                    "ps_bulk_index.hbs",
                    documents);
            indexTask.run();
        } catch (Exception e) {
            log.error("failed to index refunds: {}, cause: {}", ids, Throwables.getStackTraceAsString(e));
        }
    }


    private BulkDocument bulkDocument(RefundDocument document) {
        return new BulkDocument(refundSearchProperties.getIndexName(),
                refundSearchProperties.getIndexType(),
                document.getId(),
                JsonMapper.nonEmptyMapper().toJson(document));
    }

    private RefundDocument buildDocument(Long id) {
        try {
            Response<Refund> r = refundReadService.findById(id);
            if (!r.isSuccess() || r.getResult() == null) {
                log.error("failed to find refund by id {}, cause: {}", id, r.getError());
                return null;
            }
            return refundDocumentBuilder.build(r.getResult());
        } catch (Exception e) {
            log.error("failed to build refund record by id {}, cause: {}", id, Throwables.getStackTraceAsString(e));
            return null;
        }
    }
}
