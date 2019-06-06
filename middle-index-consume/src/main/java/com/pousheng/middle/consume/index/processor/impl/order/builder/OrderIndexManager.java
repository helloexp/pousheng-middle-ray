package com.pousheng.middle.consume.index.processor.impl.order.builder;

import com.google.common.base.Throwables;
import com.pousheng.middle.consume.index.configuration.OrderSearchProperties;
import com.pousheng.middle.consume.index.processor.impl.BulkDocument;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
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
 * @date 2019-05-16 15:23<br/>
 */
@Slf4j
@Component
public class OrderIndexManager {
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    private final OrderDocumentBuilder orderDocumentBuilder;
    private final OrderSearchProperties orderSearchProperties;
    private final IndexTaskBuilder indexTaskBuilder;
    private final Indexer indexer;

    public OrderIndexManager(OrderSearchProperties orderSearchProperties,
                             IndexTaskBuilder indexTaskBuilder,
                             Indexer indexer) {
        this.indexer = indexer;
        this.orderDocumentBuilder = new OrderDocumentBuilder();
        this.orderSearchProperties = orderSearchProperties;
        this.indexTaskBuilder = indexTaskBuilder;
    }

    public void index(Long orderId) {
        try {
            // 更新
            OrderDocument orderDocument = buildDocument(orderId);
            if (orderDocument == null) {
                return;
            }

            IndexTask indexTask = indexTaskBuilder.indexName(orderSearchProperties.getIndexName())
                    .indexType(orderSearchProperties.getIndexType())
                    .indexAction(IndexAction.INDEX).build(orderDocument.getId(), orderDocument);
            indexTask.run();
        } catch (Exception e) {
            log.error("failed to index order: {}, cause: {}", orderId, Throwables.getStackTraceAsString(e));
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
                    orderSearchProperties.getIndexName(),
                    orderSearchProperties.getIndexType(),
                    "ps_bulk_index.hbs",
                    documents);
            indexTask.run();
        } catch (Exception e) {
            log.error("failed to index order: {}, cause: {}", ids, Throwables.getStackTraceAsString(e));
        }
    }

    private BulkDocument bulkDocument(OrderDocument document) {
        return new BulkDocument(orderSearchProperties.getIndexName(),
                orderSearchProperties.getIndexType(),
                document.getId(),
                JsonMapper.nonEmptyMapper().toJson(document));
    }

    private OrderDocument buildDocument(Long orderId) {
        try {
            Response<ShopOrder> r = shopOrderReadService.findById(orderId);
            if (!r.isSuccess()) {
                log.error("failed to find order by id: {}, cause: {}", orderId, r.getError());
                return null;
            }
            return orderDocumentBuilder.create(r.getResult());
        } catch (Exception e) {
            log.error("fail to build order document from id {}, cause:{}", orderId, Throwables.getStackTraceAsString(e));
            return null;
        }
    }
}
