package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import com.pousheng.middle.web.events.warehouse.StockPushLogic;
import com.pousheng.middle.web.redis.RedisQueueProvider;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.item.dto.ParanaSkuStock;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-14
 */
@Component
@Slf4j
public class StockPusher {


    @RpcConsumer
    private MappingReadService mappingReadService;

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

    @RpcConsumer
    private StockPushLogic stockPushLogic;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private MessageSource messageSource;

    private LoadingCache<String, Long> skuCodeCacher;

    private LoadingCache<Long, OpenShop> openShopCacher;

    private ExecutorService executorService;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RedisQueueProvider redisQueueProvider;

    @Autowired
    public StockPusher(@Value("${index.queue.size: 120000}") int queueSize,
                       @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 6, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(queueSize), (new ThreadFactoryBuilder()).setNameFormat("stock-push-%d").build(),
                new RejectedExecutionHandler() {
                    @Override
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
        this.openShopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration * 24, TimeUnit.MINUTES)
                .maximumSize(2000)
                .build(new CacheLoader<Long, OpenShop>() {
                    @Override
                    public OpenShop load(Long shopId) throws Exception {
                        Response<OpenShop> r = openShopReadService.findById(shopId);
                        if (!r.isSuccess()) {
                            log.error("failed to find openShop(shopId={}),error code:{}", shopId, r.getError());
                            throw new ServiceException("openShop.find.fail");
                        }
                        return r.getResult();
                    }
                });
    }

    //todo: 可能的优化: 只需要推送本次仓库影响的店铺范围
    public void submit(List<String> skuCodes) {
        Table<Long, String, Integer> shopSkuStock = HashBasedTable.create();
        for (String skuCode : skuCodes) {
            Long spuId = skuCodeCacher.getUnchecked(skuCode);
            //找到对应的店铺id, 这些店铺需要进行库存推送
            Response<List<Long>> r = mappingReadService.findShopIdsFromItemMappingByItemId(spuId);
            if (!r.isSuccess()) {
                log.error("failed to find out shops for spu(id={}) where skuCode={}, error code:{}",
                        spuId, skuCode, r.getError());
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
                    if (shopStockRule.getStatus() < 0) {//非启用状态
                        continue;
                    }

                    if (shopStockRule.getSafeStock() >= stock) {
                        log.warn("shop(id={}) has reached safe stock({}) for sku(code={}), current stock is:{}",
                                shopId, shopStockRule.getSafeStock(), skuCode, stock);
                        Long lastPushStock = shopStockRule.getLastPushStock();
                        stock = 0L;
                    }

                    //按照设定的比例确定推送数量
                    stock = stock * shopStockRule.getRatio() / 100;
                    //判断店铺是否是官网的
                    OpenShop openShop = openShopCacher.getUnchecked(shopId);
                    if (Objects.equals(openShop.getChannel(), MiddleChannel.OFFICIAL.getValue())) {
                        shopSkuStock.put(shopId, skuCode, Math.toIntExact(stock));
                    } else {
                        //库存推送-----第三方只支持单笔更新库存,使用线程池并行处理
                        this.prallelUpdateStock(skuCode, shopId, stock, shopStockRule.getShopName());
                    }


                } catch (Exception e) {
                    log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                            skuCode, shopId, Throwables.getStackTraceAsString(e));
                }
            }
        }
        //官网批量推送
        Map<Long, Map<String, Integer>> shopSkuStockMap = shopSkuStock.rowMap();
        for (Long shopId : shopSkuStockMap.keySet()) {
            List<ParanaSkuStock> paranaSkuStocks = Lists.newArrayList();
            Map<String, Integer> skuStockMap = shopSkuStockMap.get(shopId);
            for (String skuCode : skuStockMap.keySet()) {
                ParanaSkuStock paranaSkuStock = new ParanaSkuStock();
                paranaSkuStock.setSkuCode(skuCode);
                paranaSkuStock.setStock(skuStockMap.get(skuCode));
                paranaSkuStocks.add(paranaSkuStock);
            }
            Response<Boolean> r = itemServiceCenter.batchUpdateSkuStock(shopId, paranaSkuStocks);
            if (!r.isSuccess()) {
                log.error("failed to push stocks {} to shop(id={}), error code{}",
                        paranaSkuStocks, shopId, r.getError());
            }
            //添加日志到缓存中
            List<StockPushLog> stockPushLogs = Lists.newArrayList();
            for (ParanaSkuStock paranaSkuStock:paranaSkuStocks){
                StockPushLog stockPushLog = new StockPushLog();
                stockPushLog.setShopId(shopId);
                stockPushLog.setShopName(openShopCacher.getUnchecked(shopId).getShopName());
                stockPushLog.setSkuCode(paranaSkuStock.getSkuCode());
                stockPushLog.setQuantity((long) paranaSkuStock.getStock());
                stockPushLog.setStatus(r.isSuccess() ? 1 : 2);
                stockPushLog.setCause(r.isSuccess() ? "" : messageSource.getMessage(r.getError(),null,Locale.CHINA));
                stockPushLogs.add(stockPushLog);
            }
            redisQueueProvider.startProvider(JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(stockPushLogs));
        }


    }

    private void prallelUpdateStock(String skuCode, Long shopId, Long stock, String shopName) {
        executorService.submit(() -> {
            Response<Boolean> rP = itemServiceCenter.updateSkuStock(shopId, skuCode, stock.intValue());
            if (!rP.isSuccess()) {
                log.error("failed to push stock of sku(skuCode={}) to shop(id={}), error code{}",
                        skuCode, shopId, rP.getError());
            }
            log.info("success to push stock(value={}) of sku(skuCode={}) to shop(id={})",
                    stock.intValue(), skuCode, shopId);
            //异步生成库存推送日志
            StockPushLog stockPushLog = new StockPushLog();
            stockPushLog.setShopId(shopId);
            stockPushLog.setShopName(shopName);
            stockPushLog.setSkuCode(skuCode);
            stockPushLog.setQuantity((long) stock.intValue());
            stockPushLog.setStatus(rP.isSuccess() ? 1 : 2);
            stockPushLog.setCause(rP.isSuccess() ? "" : rP.getError());
            redisQueueProvider.startProvider(JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(Lists.newArrayList(stockPushLog)));
        });
    }

  /*  @PreDestroy
    public void shutdown() {
        this.executorService.shutdown();
    }*/

}
