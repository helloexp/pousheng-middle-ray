package com.pousheng.middle.consume.index.processor.impl.refund.builder;

import com.google.common.base.Throwables;
import com.pousheng.middle.consume.index.configuration.RefundSearchProperties;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.search.api.IndexExecutor;
import io.terminus.search.api.IndexTaskBuilder;
import io.terminus.search.api.model.IndexAction;
import io.terminus.search.api.model.IndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final IndexExecutor indexExecutor;

    public RefundIndexManager(RefundSearchProperties refundSearchProperties, IndexTaskBuilder indexTaskBuilder, IndexExecutor indexExecutor) {
        this.refundSearchProperties = refundSearchProperties;
        this.indexTaskBuilder = indexTaskBuilder;
        this.indexExecutor = indexExecutor;
        refundDocumentBuilder = new RefundDocumentBuilder();
    }

    public void index(Long id) {
        Response<Refund> r = refundReadService.findById(id);
        if (!r.isSuccess()) {
            log.error("failed to find refund by id {}, cause: {}", id, r.getError());
            return;
        }
        Refund refund = r.getResult();

        try {
            if (refund == null) {
                log.error("refund not found by id {}", id);
                return;
            }

            RefundDocument refundDocument = refundDocumentBuilder.build(refund);
            IndexTask task = indexTaskBuilder.indexName(refundSearchProperties.getIndexName())
                    .indexType(refundSearchProperties.getIndexType())
                    .indexAction(IndexAction.INDEX)
                    .build(id, refundDocument);
            indexExecutor.submit(task);
        } catch (Exception e) {
            log.error("failed to index reund {}, cause: {}", refund, Throwables.getStackTraceAsString(e));
        }
    }
}
