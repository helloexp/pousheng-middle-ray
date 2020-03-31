package com.pousheng.middle.consume.index.processor.impl.sendRule;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-13 11:48<br/>
 */
@Slf4j
@Component
@Processor("pousheng_warehouse_shop_groups")
public class StockShopProcessor implements IndexEventProcessor {

    @RpcConsumer(version = "1.0.0")
    private WarehouseRuleItemReadService warehouseRuleItemReadService;
    @RpcConsumer(version = "1.0.0")
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    private final OpenShopCacher openShopCacher;
    private final WarehouseCacher warehouseCacher;
    private final WarehouseRuleCacher warehouseRuleCacher;
    private final StockSendIndexManager stockSendIndexManager;
    private final IDEventProcessor idEventProcessor;

    public StockShopProcessor(OpenShopCacher openShopCacher,
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
                log.info("规则ID:{}", id);
                // 获取店铺
                Response<WarehouseShopGroup> r = warehouseShopGroupReadService.findById(id);
                if (!r.isSuccess() || r.getResult() == null) {
                    log.error("failed to to find warehouse group shop by id {}, cause{}", id, r.getError());
                    continue;
                }
                WarehouseShopGroup shopGroup = r.getResult();
                OpenShop openShop = openShopCacher.findById(shopGroup.getShopId());
                // 暂不处理唯品会
//                if (openShop == null || "vipoxo".equals(openShop.getChannel())) {
//                    continue;
//                }
                // 获取对应的仓库
                List<PoushengWarehouseRule> rules = warehouseRuleCacher.findByGroupId(shopGroup.getGroupId());
                if (CollectionUtils.isEmpty(rules)) {
                    log.warn("failed to find rule by group {}", shopGroup);
                    continue;
                }
                // 获取仓库
                Map<Long, List<PoushengWarehouseDTO>> result = Maps.newHashMap();
                for (PoushengWarehouseRule rule : rules) {
                    Response<List<WarehouseRuleItem>> items = warehouseRuleItemReadService.findByRuleId(rule.getId());
                    if (!items.isSuccess()) {
                        log.error("failed to find warehouse items by rule {}, cause{}", rule, items.getError());
                        continue;
                    }
                    List<PoushengWarehouseDTO> collect = items.getResult()
                            .stream()
                            .map(WarehouseRuleItem::getWarehouseId)
                            .map(warehouseCacher::findById)
                            .collect(Collectors.toList());
                    List<PoushengWarehouseDTO> poushengWarehouseDTOS = result.get(rule.getId());
                    if (poushengWarehouseDTOS != null) {
                        poushengWarehouseDTOS.addAll(collect);
                        result.put(rule.getId(), poushengWarehouseDTOS);
                    } else {
                        result.put(rule.getId(), collect);
                    }
                }
                // 批量索引
                int size = stockSendIndexManager.index(openShop, result);
                log.info("index {} documents by pousheng_warehouse_shop_groups", size);
            } catch (Exception e) {
                log.error("fail to index warehouse send rule by id: {}, cause:{}", id, Throwables.getStackTraceAsString(e));
            }
        }
    }
}
