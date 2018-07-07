package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.open.yunding.JdYunDingSyncStockLogic;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.web.events.warehouse.StockPushLogic;
import com.pousheng.middle.web.redis.RedisQueueProvider;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.item.dto.ParanaSkuStock;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-14
 */
@Component
@Slf4j
// TODO 推送优化改造
public class StockPusher {


    @RpcConsumer
    private MappingReadService mappingReadService;

    @RpcConsumer
    private ItemServiceCenter itemServiceCenter;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;

    @RpcConsumer
    private StockPushLogic stockPushLogic;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private RedisQueueProvider redisQueueProvider;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;


    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;

    private LoadingCache<String, Long> skuCodeCacher;

    private LoadingCache<Long, OpenShop> openShopCacher;

    private ExecutorService executorService;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdYunDingSyncStockLogic jdYunDingSyncStockLogic;

    @Autowired
    public StockPusher(@Value("${index.queue.size: 120000}") int queueSize,
                       @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors()*2, Runtime.getRuntime().availableProcessors()*3, 60L, TimeUnit.MINUTES,
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

    public void submit(List<String> skuCodes) {

        log.info("start to push skus: {}", skuCodes);

        Table<Long, String, Integer> shopSkuStock = HashBasedTable.create();
        //库存推送日志记录
        final List<StockPushLog> thirdStockPushLogs = new CopyOnWriteArrayList<>();
        for (String skuCode : skuCodes) {
            try {
                Long spuId = skuCodeCacher.getUnchecked(skuCode);
                //找到对应的店铺id, 这些店铺需要进行库存推送
                Response<List<Long>> r = mappingReadService.findShopIdsFromItemMappingByItemId(spuId);
                if (!r.isSuccess()) {
                    log.error("failed to find out shops for spu(id={}) where skuCode={}, error code:{}",
                            spuId, skuCode, r.getError());
                    continue;
                }
                //计算库存分配并将库存推送到每个外部店铺去
                List<Long> shopIds = r.getResult();
                for (Long shopId : shopIds) {
                    log.info("start to push sku to shop: {}", shopId);
                    try {
                        if (Objects.equals(shopId,mposOpenShopId)){
                            continue;
                        }

                        //判断当前skuCode是否在当前店铺卖，如果不卖则跳过
                        Response<com.google.common.base.Optional<ItemMapping>> optionalRes = mappingReadService.findBySkuCodeAndOpenShopId(skuCode,shopId);
                        if (!optionalRes.isSuccess()){
                            log.error("find item mapping by sku code:{} shop id:{} ,error:{}",skuCode,shopId,optionalRes.getError());
                            continue;
                        }

                        com.google.common.base.Optional<ItemMapping> mappingOptional = optionalRes.getResult();

                        if (!mappingOptional.isPresent()){
                            log.warn("current shop id:{} not sale sku code:{} so skip",shopId,skuCode);
                            continue;
                        }

                        Response<WarehouseShopStockRule> rShopStockRule = warehouseShopRuleClient.findByShopIdAndSku(shopId, skuCode);
                        if (!rShopStockRule.isSuccess()) {
                            log.warn("failed to find shop stock push rule for shop(id={}), error code:{}",
                                    shopId, rShopStockRule.getError());
                            continue;
                        }
                        //和安全库存进行比较, 确定推送库存数量
                        WarehouseShopStockRule shopStockRule = rShopStockRule.getResult();
                        if (shopStockRule.getStatus() < 0) { //非启用状态
                            continue;
                        }

                        //计算每个店铺的可用库存
                        Response<List<Long>> rWarehouseIds = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
                        if (!rWarehouseIds.isSuccess()) {
                            log.error("find warehouse list by shopId fail: shopId: {}, caused: {]",shopId, rWarehouseIds.getError());
                            continue;
                        }

                        long start1 = System.currentTimeMillis();
                        Response<List<AvailableInventoryDTO>> getRes = inventoryClient.getAvailInvRetNoWarehouse(Lists.newArrayList(
                                Lists.transform(rWarehouseIds.getResult(), input -> AvailableInventoryRequest.builder().skuCode(skuCode).warehouseId(input).build())
                        ), shopId);
                        long end1 = System.currentTimeMillis();
                        log.info("get available inventory cost: {}", (end1-start1));
                        if (!getRes.isSuccess()) {
                            log.error("error to find available inventory quantity: shopId: {}, caused: {]",shopId, getRes.getError());
                            continue;
                        }
                        Long channelStock = 0L;
                        Long shareStock = 0L;
                        if (!ObjectUtils.isEmpty(getRes.getResult())) {
                            channelStock = getRes.getResult().stream().mapToLong(AvailableInventoryDTO::getChannelRealQuantity).sum();
                            shareStock = getRes.getResult().stream().mapToLong(AvailableInventoryDTO::getInventoryUnAllocQuantity).sum();
                        }
                        log.info("search sku stock by skuCode is {},shopId is {},channelStock is {},shareStock is {}",
                                skuCode, shopId, channelStock, shareStock);

                        //如果库存数量小于0则推送0
                        if (channelStock < 0L){
                            log.warn("shop(id={}) channelStock is less than 0 for sku(code={}), current channelStock is:{}, shareStock is:{}",
                                    shopId, skuCode, channelStock, shareStock);

                            channelStock = 0L;
                        }
                        if (shareStock < 0L){
                            log.warn("shop(id={}) shareStock is less than 0 for sku(code={}), current channelStock is:{}, shareStock is:{}",
                                    shopId, skuCode, channelStock, shareStock);

                            shareStock = 0L;
                        }

                        if (null != shopStockRule.getSafeStock()) {
                            shareStock = Math.max(0, shareStock - shopStockRule.getSafeStock());
                        }

                        //按照设定的比例确定推送数量
                        Long stock = Math.max(0,
                                channelStock
                                        + shareStock * shopStockRule.getRatio() / 100
                                        + (null == shopStockRule.getJitStock() ? 0 : shopStockRule.getJitStock())
                        );

                        log.info("after calculate, push stock quantity (skuCode is {},shopId is {}), is {}",
                                skuCode, shopId, stock);

                        //判断店铺是否是官网的
                        OpenShop openShop = openShopCacher.getUnchecked(shopId);
                        if (Objects.equals(openShop.getChannel(), MiddleChannel.OFFICIAL.getValue())) {
                            log.info("start to push to official shop: {}, with quantity: {}", openShop, stock);
                            shopSkuStock.put(shopId, skuCode, Math.toIntExact(stock));
                        } else {
                            log.info("start to push to third part shop: {}, with quantity: {}", openShop, stock);
                            //库存推送-----第三方只支持单笔更新库存,使用线程池并行处理
                            log.info("parall update stock start");
                            this.prallelUpdateStock(skuCode, shopId, stock, shopStockRule.getShopName());
                            log.info("parall update stock return");
                        }

                    } catch (Exception e) {
                        log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                                skuCode, shopId, Throwables.getStackTraceAsString(e));
                    }
                }
            } catch (Exception e) {
                log.error("failed to push stock,sku is {}", skuCode);
            }
        }

        //官网批量推送
        log.info("send to parana by parall update stock start");
        sendToParana(shopSkuStock);
        log.info("send to parana by parall update stock return");
        //库存日志推送
        if (!thirdStockPushLogs.isEmpty()) {
            thirdStockPushLogs.forEach(item->{
                log.info("stock push third shop log info:{}",item.toString());
            });
        }

        //库存日志推送
        if (!thirdStockPushLogs.isEmpty()) {
            redisQueueProvider.startProvider(JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(thirdStockPushLogs));
        }


    }

    private void prallelUpdateStock(String skuCode, Long shopId, Long stock, String shopName) {
        executorService.submit(() -> {
            List<StockPushLog> stockPushLogs = Lists.newArrayList();
            OpenShop openShop = openShopCacher.getUnchecked(shopId);
            Map<String, String> extra = openShop.getExtra();
            Response<Boolean> rP = null;
            if (extra.get("isYunDing") != null && Objects.equals(extra.get("isYunDing"), "true")) {
                rP = jdYunDingSyncStockLogic.syncJdYundingStock(shopId, skuCode, Math.toIntExact(stock));
                if (!rP.isSuccess()) {
                    log.error("failed to push isYunDing stock of sku(skuCode={}) to shop(id={}), error code{}",
                            skuCode, shopId, rP.getError());
                }
            } else {
                rP = itemServiceCenter.updateSkuStock(shopId, skuCode, stock.intValue());
                if (!rP.isSuccess()) {
                    log.error("failed to push stock of sku(skuCode={}) to shop(id={}), error code{}",
                            skuCode, shopId, rP.getError());
                }
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
            stockPushLog.setSyncAt(new Date());
            stockPushLogs.add(stockPushLog);
            //库存日志推送
            log.info("stock push third shop log info:{}",stockPushLogs.toString());
        });
    }


    private void sendToParana(Table<Long, String, Integer> shopSkuStock) {
        executorService.submit(() -> {
            Map<Long, Map<String, Integer>> shopSkuStockMap = shopSkuStock.rowMap();
            for (Long shopId : shopSkuStockMap.keySet()) {
                try {
                    List<ParanaSkuStock> paranaSkuStocks = Lists.newArrayList();
                    Map<String, Integer> skuStockMap = shopSkuStockMap.get(shopId);
                    for (String skuCode : skuStockMap.keySet()) {
                        ParanaSkuStock paranaSkuStock = new ParanaSkuStock();
                        paranaSkuStock.setSkuCode(skuCode);
                        paranaSkuStock.setStock(skuStockMap.get(skuCode));
                        paranaSkuStocks.add(paranaSkuStock);
                    }
                    log.info("search sku stock by shopId  is {},paranaSkuStocks is {}", shopId, paranaSkuStocks);
                    Response<Boolean> r = itemServiceCenter.batchUpdateSkuStock(shopId, paranaSkuStocks);
                    if (!r.isSuccess()) {
                        log.error("failed to push stocks {} to shop(id={}), error code{}",
                                paranaSkuStocks, shopId, r.getError());
                    }
                } catch (Exception e) {
                    log.error("sync offical stock failed,caused by {}", Throwables.getStackTraceAsString(e));
                }
            }

        });
    }
}
