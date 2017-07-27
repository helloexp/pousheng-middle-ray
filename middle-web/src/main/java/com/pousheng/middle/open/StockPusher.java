package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.item.service.PushedItemReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-14
 */
@Component
@Slf4j
public class StockPusher {

    private final ExecutorService executorService;

    @RpcConsumer
    private PushedItemReadService pushedItemReadService;

    @RpcConsumer
    private ItemServiceCenter itemServiceCenter;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private AvailableStockCalc availableStockCalc;

    @RpcConsumer
    private WarehouseShopStockRuleReadService warehouseShopStockRuleReadService;

    @RpcConsumer
    private WarehouseShopStockRuleWriteService warehouseShopStockRuleWriteService;

    private LoadingCache<String, Long> skuCodeCacher;

    @Autowired
    public StockPusher(@Value("${index.queue.size: 10000}") int queueSize,
                       @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors()*6, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(queueSize), (new ThreadFactoryBuilder()).setNameFormat("stock-update-%d").build(),
                new RejectedExecutionHandler() {
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("task {} is rejected", r);
                    }
                });

        this.skuCodeCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration * 24, TimeUnit.MINUTES)
                .maximumSize(200000)
                .build(new CacheLoader<String, Long>() {
                    @Override
                    public Long load(String skuCode) throws Exception {
                        Response<List<SkuTemplate>> r = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
                        if (!r.isSuccess()) {
                            log.error("failed to find skuTemplate(skuCode={}),error code:{}", skuCode, r.getError());
                            throw new ServiceException("skuTemplate.find.fail");
                        }
                        List<SkuTemplate> skuTemplates = r.getResult();
                        if (CollectionUtils.isEmpty(skuTemplates)) {
                            log.error("skuTemplate(skuCode={}) not found", skuCode);
                            throw new ServiceException("skuTemplate.not.found");
                        }
                        return skuTemplates.get(0).getSpuId();
                    }
                });
    }

    //todo: 可能的优化: 只需要推送本次仓库影响的店铺范围
    public void submit(final String skuCode) {
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {
                Long spuId = skuCodeCacher.getUnchecked(skuCode);
                //找到对应的店铺id, 这些店铺需要进行库存推送
                Response<List<Long>> r = pushedItemReadService.findPushedOpenShopIdsOfItem(spuId);
                if (!r.isSuccess()) {
                    log.error("failed to find out shops for spu(id={}), error code:{}", spuId, r.getError());
                    return;
                }

                //计算库存分配并将库存推送到每个外部店铺去
                List<Long> shopIds = r.getResult();
                for (Long shopId : shopIds) {
                    try {
                        //计算每个店铺的可用库存
                        Long stock = availableStockCalc.availableStock(shopId, skuCode);
                        Response<WarehouseShopStockRule> rShopStockRule = warehouseShopStockRuleReadService.findByShopId(shopId);
                        if (!rShopStockRule.isSuccess()) {
                            log.error("failed to find shop stock push rule for shop(id={}), error code:{}",
                                    shopId, rShopStockRule.getError());
                            return;
                        }
                        //和安全库存进行比较, 确定推送库存数量
                        WarehouseShopStockRule shopStockRule = rShopStockRule.getResult();
                        if(shopStockRule.getStatus()<0){//非启用状态
                            return;
                        }
                        //按照设定的比例确定推送数量
                        stock = stock * shopStockRule.getRatio() / 100;
                        if (shopStockRule.getSafeStock() >= stock) {
                            log.warn("shop(id={}) has reached safe stock({}), current stock is:{}",
                                    shopId, shopStockRule.getSafeStock(), stock);
                            Long lastPushStock = shopStockRule.getLastPushStock();
                            if (lastPushStock != null && lastPushStock <= shopStockRule.getSafeStock()) {
                                log.info("skip to rePush stock to shop(id={}), since it has already reach safe stock before", shopId);
                                return;
                            } else {//如果本次可用库存低于安全库存, 则推送0
                                stock = 0L;
                            }
                        }
                        //库存推送
                        Response<Boolean> rP = itemServiceCenter.updateSkuStock(shopId, skuCode, stock.intValue());
                        if (!rP.isSuccess()) {
                            log.error("failed to push stock of sku(skuCode={}) to shop(id={}), error code{}",
                                    skuCode, shopId, rP.getError());
                        }
                        //更新上次推送的可用库存
                        WarehouseShopStockRule u = new WarehouseShopStockRule();
                        u.setId(shopStockRule.getId());
                        u.setLastPushStock(stock);
                        Response<Boolean> rRule = warehouseShopStockRuleWriteService.update(u);
                        if (!rRule.isSuccess()) {
                            log.error("failed to update lastPushStock to {} for {}, error code:{}", stock, u);
                        }
                    } catch (Exception e) {
                        log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                                skuCode, shopId, Throwables.getStackTraceAsString(e));
                    }
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        this.executorService.shutdown();
    }

}
