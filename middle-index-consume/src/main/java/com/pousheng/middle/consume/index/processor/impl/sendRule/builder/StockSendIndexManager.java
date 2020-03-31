package com.pousheng.middle.consume.index.processor.impl.sendRule.builder;

import com.google.common.collect.Lists;
import com.pousheng.inventory.domain.dto.PoushengWarehouseDTO;
import com.pousheng.middle.consume.index.cacher.ShopCacher;
import com.pousheng.middle.consume.index.configuration.StockSendSearchProperties;
import com.pousheng.middle.consume.index.processor.impl.BulkDocument;
import com.pousheng.middle.consume.index.processor.impl.sendRule.dto.StockSendDocument;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.shop.model.Shop;
import io.terminus.search.api.Indexer;
import io.terminus.search.api.model.BulkIndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 14:47<br/>
 */
@Slf4j
@Component
public class StockSendIndexManager {
    private final ShopCacher shopCacher;
    private final StockSendBuilder stockSendBuilder;
    private final StockSendSearchProperties stockSendSearchProperties;
    private final Indexer indexer;

    public StockSendIndexManager(ShopCacher shopCacher, StockSendBuilder stockSendBuilder, StockSendSearchProperties stockSendSearchProperties, Indexer indexer) {
        this.shopCacher = shopCacher;
        this.stockSendBuilder = stockSendBuilder;
        this.stockSendSearchProperties = stockSendSearchProperties;
        this.indexer = indexer;
    }

    /**
     * 根据仓库与仓库关联店铺批量索引
     */
    public int index(Long ruleId, PoushengWarehouseDTO warehouse, List<OpenShop> shops) {
        Shop s = shopCacher.findByCompanyAndCode(warehouse.getCompanyId(), warehouse.getOutCode());
        List<BulkDocument> docs = Lists.newArrayListWithCapacity(shops.size());
        for (OpenShop shop : shops) {
            StockSendDocument doc = stockSendBuilder.build(ruleId, joinKey(shop.getId(), warehouse.getId()), s, shop, warehouse);
            docs.add(bulkDocument(doc));
        }
        bulkIndex(docs);
        return docs.size();
    }

    /**
     * 根据店铺与店铺关联仓库批量索引
     */
    public int index(OpenShop shop, Map<Long, List<PoushengWarehouseDTO>> result) {
        Set<Long> ruleIds = result.keySet();
        List<BulkDocument> docs = Lists.newArrayList();
        for (Long ruleId : ruleIds) {
            List<PoushengWarehouseDTO> poushengWarehouseDTOS = result.get(ruleId);
            for (PoushengWarehouseDTO warehouse : poushengWarehouseDTOS) {
                Shop s = shopCacher.findByCompanyAndCode(warehouse.getCompanyId(), warehouse.getOutCode());
                StockSendDocument doc = stockSendBuilder.build(ruleId, joinKey(shop.getId(), warehouse.getId()), s, shop, warehouse);
                docs.add(bulkDocument(doc));
            }
        }
        bulkIndex(docs);
        return docs.size();
    }

    private void bulkIndex(List<BulkDocument> docs) {
        if (CollectionUtils.isEmpty(docs)) {
            return;
        }
        new BulkIndexTask(indexer,
                stockSendSearchProperties.getIndexName(),
                stockSendSearchProperties.getIndexType(),
                "ps_bulk_index.hbs",
                docs)
                .run();
    }

    private BulkDocument bulkDocument(StockSendDocument document) {
        return new BulkDocument(
                stockSendSearchProperties.getIndexName(),
                stockSendSearchProperties.getIndexType(),
                document.getId(),
                JsonMapper.nonEmptyMapper().toJson(document));
    }

    private String joinKey(Long shopId, Long warehouseId) {
        return shopId + "-" + warehouseId;
    }
}
