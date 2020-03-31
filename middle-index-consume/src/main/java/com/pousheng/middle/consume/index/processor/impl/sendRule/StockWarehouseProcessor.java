package com.pousheng.middle.consume.index.processor.impl.sendRule;

import com.google.common.base.Throwables;
import com.google.common.primitives.Longs;
import com.pousheng.inventory.api.service.WarehouseRuleItemReadService;
import com.pousheng.inventory.api.service.WarehouseShopGroupReadService;
import com.pousheng.inventory.domain.dto.PoushengWarehouseDTO;
import com.pousheng.inventory.domain.model.PoushengWarehouseRule;
import com.pousheng.inventory.domain.model.WarehouseRuleItem;
import com.pousheng.inventory.domain.model.WarehouseShopGroup;
import com.pousheng.middle.consume.index.cacher.OpenShopCacher;
import com.pousheng.middle.consume.index.cacher.WarehouseCacher;
import com.pousheng.middle.consume.index.cacher.WarehouseRuleCacher;
import com.pousheng.middle.consume.index.configuration.StockSendSearchProperties;
import com.pousheng.middle.consume.index.processor.core.IDEventProcessor;
import com.pousheng.middle.consume.index.processor.core.IndexEvent;
import com.pousheng.middle.consume.index.processor.core.IndexEventProcessor;
import com.pousheng.middle.consume.index.processor.core.Processor;
import com.pousheng.middle.consume.index.processor.impl.sendRule.builder.StockSendIndexManager;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-13 11:48<br/>
 */
@Slf4j
@Component
@Processor("pousheng_warehouse_rule_items")
public class StockWarehouseProcessor implements IndexEventProcessor {

    @RpcConsumer(version = "1.0.0")
    private WarehouseRuleItemReadService warehouseRuleItemReadService;
    @RpcConsumer(version = "1.0.0")
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    private final OpenShopCacher openShopCacher;
    private final WarehouseCacher warehouseCacher;
    private final WarehouseRuleCacher warehouseRuleCacher;
    private final StockSendIndexManager stockSendIndexManager;
    private final IDEventProcessor idEventProcessor;

    public StockWarehouseProcessor(OpenShopCacher openShopCacher,
                                   WarehouseCacher warehouseCacher,
                                   WarehouseRuleCacher warehouseRuleCacher,
                                   StockSendIndexManager stockSendIndexManager,
                                   StockSendSearchProperties stockSendSearchProperties) {
        this.openShopCacher = openShopCacher;
        this.warehouseCacher = warehouseCacher;
        this.warehouseRuleCacher = warehouseRuleCacher;
        this.stockSendIndexManager = stockSendIndexManager;
        idEventProcessor = new IDEventProcessor(stockSendSearchProperties.getIndexName(),
                row -> Longs.tryParse(row.get(0)),
                this::doProcess);
    }

    @Override
    public void process(IndexEvent event) {
        if (CollectionUtils.isEmpty(event.getData())) {
            return;
        }

        idEventProcessor.process(event);
    }

    public void doProcess(Set<Long> ids) {
        for (Long id : ids) {
            try {
                Response<WarehouseRuleItem> r = warehouseRuleItemReadService.findById(id);
                if (!r.isSuccess()) {
                    log.error("failed to find warehouse rule item by id {}, cause{}", id, r.getError());
                    continue;
                }
                WarehouseRuleItem ruleItem = r.getResult();
                // 获取对应的仓库
                PoushengWarehouseRule rule = warehouseRuleCacher.findById(ruleItem.getRuleId());
                if (rule == null) {
                    log.warn("failed to find rule by id {}", ruleItem.getRuleId());
                    continue;
                }
                // 所有仓库关联的店铺
                Response<List<WarehouseShopGroup>> shopR = warehouseShopGroupReadService.findByGroupId(rule.getShopGroupId());
                if (!shopR.isSuccess()) {
                    log.error("failed to find shops by rule {}, cause{}", rule, shopR.getError());
                    continue;
                }
                if (CollectionUtils.isEmpty(shopR.getResult())) {
                    continue;
                }

                List<OpenShop> shops = shopR.getResult().stream()
                        .map(it -> openShopCacher.findById(it.getShopId()))
                //       .filter(it -> !"vipoxo".equals(it.getChannel()))
                        .collect(Collectors.toList());
                if (shops.isEmpty()) {
                    return;
                }
                // 批量索引
                PoushengWarehouseDTO warehouse = warehouseCacher.findById(ruleItem.getWarehouseId());
                int size = stockSendIndexManager.index(rule.getId(), warehouse, shops);
                log.info("index {} documents by pousheng_warehouse_rule_items", size);
            } catch (Exception e) {
                log.error("fail to index warehouse send rule by id: {}, cause:{}", id, Throwables.getStackTraceAsString(e));
            }
        }
    }
}
