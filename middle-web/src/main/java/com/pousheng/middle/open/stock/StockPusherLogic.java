package com.pousheng.middle.open.stock;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.open.stock.yunju.YjStockPushClient;
import com.pousheng.middle.open.stock.yunju.dto.StockPushLogStatus;
import com.pousheng.middle.open.stock.yunju.dto.YjStockInfo;
import com.pousheng.middle.open.stock.yunju.dto.YjStockRequest;
import com.pousheng.middle.open.yunding.JdYunDingSyncStockLogic;
import com.pousheng.middle.order.dto.ShipmentItemCriteria;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.model.SkuOrderLockStock;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.enums.ShopOpeningStatus;
import com.pousheng.middle.shop.enums.ShopType;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.web.item.cacher.ItemGroupCacher;
import com.pousheng.middle.web.item.cacher.ShopGroupRuleCacher;
import com.pousheng.middle.web.item.cacher.WarehouseGroupRuleCacher;
import com.pousheng.middle.web.middleLog.dto.StockLogDto;
import com.pousheng.middle.web.middleLog.dto.StockLogTypeEnum;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.redis.RedisQueueProvider;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.item.dto.ParanaSkuStock;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/9/13
 */
@Component
@Slf4j
public class StockPusherLogic {

    public ExecutorService executorService;
    public LoadingCache<String, Long> skuCodeCacher;
    public LoadingCache<Long, OpenShop> openShopCacher;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private TerminusMQProducer producer;
    @Autowired
    private StockPushCacher stockPushCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    YjStockPushClient yjStockPushClient;
    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;

    private static final String SHOP_CODE = "hkPerformanceShopCode";
    private static final Integer PUSH_SIZE = 500;
    private static final Long PUSH_ZERO = 0L;
    private static final Integer HUNDRED = 100;

    @Value("${terminus.rocketmq.stockLogTopic}")
    private String stockLogTopic;
    @Setter
    @Value("${stock.push.cache.enable: true}")
    private boolean stockPusherCacheEnable;

    @RpcConsumer
    private MappingReadService mappingReadService;

    @RpcConsumer
    private ItemServiceCenter itemServiceCenter;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private RedisQueueProvider redisQueueProvider;


    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;

    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private ShopGroupRuleCacher shopGroupRuleCacher;
    @Autowired
    private WarehouseGroupRuleCacher warehouseGroupRuleCacher;
    @Autowired
    private ItemGroupCacher itemGroupCacher;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;


    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    @Autowired
    private JdYunDingSyncStockLogic jdYunDingSyncStockLogic;

    @Autowired
    private MiddleOrderReadService middleOrderReadService;


    @Autowired
    public StockPusherLogic(@Value("${index.queue.size: 120000}") int queueSize,
                            @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 3, 60L, TimeUnit.MINUTES,
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


    public void createAndPushLogs(List<StockPushLog> logs, String skuCode, Long shopId, String channelSkuId, Long stock, Boolean status, String msg) {
        try {
            OpenShop shop = openShopCacher.get(shopId);
            String shopCode = shop.getExtra().get(SHOP_CODE);
            if (StringUtils.isEmpty(shopCode)) {
                shopCode = Splitter.on("-").splitToList(shop.getAppKey()).get(1);
            }
            StockPushLog stockPushLog = new StockPushLog().shopId(shopId)
                    .shopName(shop.getShopName())
                    .outId(shopCode)
                    .skuCode(skuCode)
                    .channelSkuId(channelSkuId)
                    .quantity(stock)
                    .status(status ? 1 : 2)
                    .cause(msg).syncAt(new Date());
            logs.add(stockPushLog);
            if (!logs.isEmpty() && logs.size() % PUSH_SIZE == 0) {
                pushLogs(logs);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void pushLogs(List<StockPushLog> logs) {
        log.info("start to push middle to shop stock log");
        if (!logs.isEmpty()) {
            String logJson = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(logs);
            producer.send(stockLogTopic, new StockLogDto().logJson(logJson).type(StockLogTypeEnum.MIDDLETOSHOP.value()));
            logs.clear();
        }
    }

    /**
     * @Description 云聚jit可用库存计算
     * @Date        2018/9/13
     * @param       shopId
     * @param       skuWareshouseIds
     * @param
     * @return
     */
    public Table<String,Long,Long> calculateYjStock(Long shopId, Map<String, List<Long>> skuWareshouseIds, Map<String, WarehouseShopStockRule> shopStockRules){
        Table<String,Long,Long> stockTable = HashBasedTable.create();
        //库存中心可用库存查询
        List<AvailableInventoryRequest> requests = Lists.newArrayList();
        skuWareshouseIds.forEach((skuCode,warehouseIds) ->{
            warehouseIds.forEach(warehouseId -> {
                requests.add(AvailableInventoryRequest.builder().skuCode(skuCode).warehouseId(warehouseId).build());

            });
        });


        Response<List<AvailableInventoryDTO>> getRes = inventoryClient.getAvailableInventory(requests,shopId);
        if (!getRes.isSuccess()) {
            log.error("error to find available inventory quantity: shopId: {}, caused: {]",shopId, getRes.getError());
            return null;
        }
        List<AvailableInventoryDTO> availableInventoryDTOs = getRes.getResult();

        //计算可用库存
        if (!ObjectUtils.isEmpty(getRes.getResult())) {
            availableInventoryDTOs.forEach(availableInventoryDTO -> {
                try {
                    Long stock = 0L;
                    String skuCode = availableInventoryDTO.getSkuCode();
                    Long warehouseId = availableInventoryDTO.getWarehouseId();
                    WarehouseShopStockRule shopStockRule = shopStockRules.get(skuCode);
                    Integer channelStock = availableInventoryDTO.getChannelRealQuantity();
                    Integer shareStock = availableInventoryDTO.getInventoryUnAllocQuantity();

                    //如果库存数量小于0则推送0
                    if (channelStock < 0) {
                        log.warn("shop(id={}) channelStock is less than 0 for sku(code={}), current channelStock is:{}, shareStock is:{}",
                                shopId, skuCode, channelStock, shareStock);
                        channelStock = 0;
                    }
                    if (null != shopStockRule.getSafeStock()) {
                        shareStock = shareStock - shopStockRule.getSafeStock().intValue();
                    }
                    //按照设定的比例确定推送数量
                    Long yjjitQty = 0L; //todo
                    stock = Math.max(0,
                            channelStock
                                    + shareStock * shopStockRule.getRatio() / 100
                                    + (null == shopStockRule.getJitStock() ? 0 : shopStockRule.getJitStock())
                                    + yjjitQty
                    );
                    log.info("after calculate, push stock quantity (skuCode is {},shopId is {}), is {}",
                            skuCode, shopId, stock);

                    stockTable.put(skuCode, warehouseId, stock);
                }catch (Exception e){
                    log.error("failed to calculate,case:{}",Throwables.getStackTraceAsString(e));
                }
            });
        }

        return stockTable;
    }


    /**
     * @Description 获取默认发货仓
     * @Date        2018/9/13
     * @param
     * @return
     */
    public List<Long> getWarehouseIdsByShopId(Long shopId){
        Response<List<Long>> rWarehouseIds = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
        if (!rWarehouseIds.isSuccess()) {
            log.error("find warehouse list by shopId fail: shopId: {}, caused: {]", shopId, rWarehouseIds.getError());
            return null;
        }
        return rWarehouseIds.getResult();
    }

    /**
     * @Description 获取库存推送规则
     * @Date        2018/9/13
     * @param       shopId
     * @param       skuCodes
     * @return
     */
    public Map<String,WarehouseShopStockRule> getWarehouseShopStockRules(Long shopId,List<String> skuCodes){
        //默认发货仓规则
        Map<String,WarehouseShopStockRule> shopStockRules = Maps.newHashMap();
        skuCodes.forEach(skuCode ->{
                    Response<WarehouseShopStockRule> rShopStockRule = warehouseShopRuleClient.findByShopIdAndSku(shopId, skuCode);
                    if (!rShopStockRule.isSuccess()) {
                        log.warn("failed to find shop stock push rule for shop(id={}), error code:{}",
                                shopId, rShopStockRule.getError());
                        return;
                    }
                    shopStockRules.put(skuCode,rShopStockRule.getResult());
                }
        );
        return shopStockRules;
    }

    /**
     * @Description 库存推送YJ
     * @Date        2018/7/4
     * @param       yunjuStockInfoList
     * @return
     */
    public void sendToYj(List<YjStockInfo> yunjuStockInfoList){
        this.executorService.submit(() -> {
            //List<StockPushLogEs> stockPushLogs = Lists.newArrayList();
            String traceId = UUID.randomUUID().toString().replace("-","");
            if (!yunjuStockInfoList.isEmpty()) {
                Response<Boolean> resp = yjStockPushClient.syncStocks(traceId,
                        YjStockRequest.builder().stockInfo(yunjuStockInfoList).build());
                //成功则写入缓存
                if(stockPusherCacheEnable && resp.isSuccess()){
                    yunjuStockInfoList.stream().forEach(yjStockInfo -> {
                        stockPushCacher.addToRedis(StockPushCacher.ORG_TYPE_WARE,
                                yjStockInfo.getWarehouseCode(),
                                yjStockInfo.getBarCode(),
                                yjStockInfo.getNum());
                    });
                }
                //写入推送日志
                int status = resp.isSuccess() ? StockPushLogStatus.PUSH_SUCESS.value() : StockPushLogStatus.PUSH_FAIL.value();
                String cause = resp.isSuccess() ? "" : resp.getError();

                List<StockPushLog> stockPushLogs = Lists.newArrayList();
                yunjuStockInfoList.stream().forEach(yjStockInfo -> {
                    this.createAndPushLogs(stockPushLogs,
                            yjStockInfo.getBarCode(),
                            warehouseCacher.findByCode(yjStockInfo.getWarehouseCode()).getId(),
                            null,
                            (long) yjStockInfo.getNum(),
                            resp.isSuccess(),
                            resp.getError());
                    this.pushLogs(stockPushLogs);
                });
            }
        });
    }

    /**
     * @Description 构造同步云聚请求
     * @Date        2018/7/4
     * @param       yunjuStockInfoList
     * @param       skuCode
     * @return
     */
    public void appendYjRequest(List<YjStockInfo> yunjuStockInfoList, Table<String,Long,Long> stocks) {
        int lineNo=1;
        if(Objects.isNull(stocks)){
            log.error("appendYjRequest parameter stocks is null");
            return;
        }

        Set<Table.Cell<String,Long,Long>> cells = stocks.cellSet();
        for(Table.Cell<String,Long,Long> cell:cells){
            String skuCode = cell.getRowKey();
            Long warehouseId = cell.getColumnKey();
            Long availStock = cell.getValue();
            Integer cacheStock = stockPushCacher.getFromRedis(StockPushCacher.ORG_TYPE_WARE, warehouseId.toString(), skuCode);

            WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
            String companyId= warehouse.getCompanyId();
            String outCode = warehouse.getExtra().get("outCode");

            if(!Objects.isNull(cacheStock) && availStock.intValue() == cacheStock.intValue()) {
                return;
            }
            //添加到待同步列表中
            yunjuStockInfoList.add(YjStockInfo.builder()
                    .lineNo(String.valueOf(lineNo++))
                    .companyCode(warehouse.getCompanyCode())
                    .warehouseCode(outCode)
                    .barCode(skuCode)
                    .num(availStock.intValue())
                    .build());

        }
    }

    /**
     * @Description 过滤仓库的商品分组取的可售的仓库列表
     * @Date        2018/9/13
     * @param       shopId
     * @param       skuWareshouseIds
     * @return
     */
    public Map<String,List<Long>> filterWarehouseSkuGroup(Long shopId, List<String> skuCodes, List<Long>  warehouseIds) {
        //查询商品分组
        Map<String,List<Long>> skuWareshouseIds = Maps.newHashMap();
        OpenShop openShop = openShopCacher.getUnchecked(shopId);
        String companyCode = openShop.getExtra().get("companyCode");

        skuCodes.forEach(skuCode ->{
            //skuWareshouseIds.put(skuCode, queryHkWarhouseOrShopStockApi.isVendibleWarehouse(skuCode, warehouseIds, companyCode));
            skuWareshouseIds.put(skuCode, warehouseIds); //todo
        });

        return skuWareshouseIds;
    }

    /**
     * @Description 查询店铺的商品分组取的可售的仓库列表
     * @Date        2018/9/13
     * @param       shopId
     * @param       skuWareshouseIds
     * @return
     */
    public List<String> filterShopSkuGroup(Long shopId, List<String> skuCodes) {
        //根据商品分组规则判断该店铺是否运行售卖此SKU
        return skuCodes.stream().filter(skuCode ->{
                    //return queryHkWarhouseOrShopStockApi.isVendible(skuCode, shopId);
                    return true; //todo
                }
                ).collect(Collectors.toList());
    }


    public void prallelUpdateStock(ItemMapping itemMapping, Long stock) {
        executorService.submit(() -> {
            if (log.isDebugEnabled()) {
                log.debug("start to push stock(value={}) of sku(skuCode={}) channelSku(id:{}) to shop(id={})",
                        stock.intValue(), itemMapping.getSkuCode(), itemMapping.getChannelSkuId(), itemMapping.getOpenShopId());
            }
            List<StockPushLog> stockPushLogs = Lists.newArrayList();
            OpenShop openShop = openShopCacher.getUnchecked(itemMapping.getOpenShopId());
            Map<String, String> extra = openShop.getExtra();
            Response<Boolean> rP = null;
            if (extra.get("isYunDing") != null && Objects.equals(extra.get("isYunDing"), "true")) {
                rP = jdYunDingSyncStockLogic.syncJdYundingStock(itemMapping, Math.toIntExact(stock));
                if (!rP.isSuccess()) {
                    log.error("failed to push isYunDing stock of sku(skuCode={}) to shop(id={}), error code{}",
                            itemMapping.getSkuCode(), itemMapping.getOpenShopId(), rP.getError());
                }
            } else {
                rP = itemServiceCenter.updateSkuStock(itemMapping, stock.intValue());
                if (!rP.isSuccess()) {
                    log.error("failed to push stock of sku(skuCode={}) to shop(id={}), error code{}",
                            itemMapping.getSkuCode(), itemMapping.getOpenShopId(), rP.getError());
                }
            }
            log.info("success to push stock(value={}) of sku(skuCode={}) channelSku(id:{}) to shop(id={})",
                    stock.intValue(), itemMapping.getSkuCode(), itemMapping.getChannelSkuId(), itemMapping.getOpenShopId());
            //库存日志推送
            this.createAndPushLogs(stockPushLogs, itemMapping.getSkuCode(), itemMapping.getOpenShopId(), itemMapping.getChannelSkuId(), (long) stock.intValue(), rP.isSuccess(), rP.getError());
            this.pushLogs(stockPushLogs);

            //如果推送成功则将本次推送记录写入缓存
            if(stockPusherCacheEnable && rP.isSuccess()){
                stockPushCacher.addToRedis(StockPushCacher.ORG_TYPE_SHOP, itemMapping.getOpenShopId().toString(), itemMapping.getSkuCode(), stock.intValue());
            }
        });
    }


    public void sendToParana(Table<Long, String, Integer> shopSkuStock) {
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
                    //生成库存推送日志
                    List<StockPushLog> stockPushLogs = Lists.newArrayList();
                    //库存日志推送
                    paranaSkuStocks.forEach(stock -> createAndPushLogs(stockPushLogs, stock.getSkuCode(), shopId, null, (long) stock.getStock().intValue(), r.isSuccess(), r.getError()));
                    pushLogs(stockPushLogs);

                    //如果推送成功则将本次推送记录写入缓存
                    if(stockPusherCacheEnable && r.isSuccess()){
                        paranaSkuStocks.forEach(skuStock -> {
                            stockPushCacher.addToRedis(StockPushCacher.ORG_TYPE_SHOP, shopId.toString(), skuStock.getSkuCode(), skuStock.getStock());
                        });
                    }
                } catch (Exception e) {
                    log.error("sync offical stock failed,caused by {}", Throwables.getStackTraceAsString(e));
                }
            }
        });
    }

    /**
     * @param shopId
     * @param skuCode
     * @param warehouseIds
     * @param shopStockRule
     * @return
     * @Description 查询库存中心计算可用库存
     * @Date 2018/7/11
     */
    public Long calculateStock(Long shopId, String skuCode, List<Long> warehouseIds, WarehouseShopStockRule shopStockRule) {
        Long stock = 0L;

        long start1 = System.currentTimeMillis();
        Response<List<AvailableInventoryDTO>> getRes = inventoryClient.getAvailInvRetNoWarehouse(Lists.newArrayList(
                Lists.transform(warehouseIds, input -> AvailableInventoryRequest.builder().skuCode(skuCode).warehouseId(input).build())
        ), shopId);
        long end1 = System.currentTimeMillis();
        log.info("get available inventory cost: {}", (end1 - start1));
        if (!getRes.isSuccess()) {
            log.error("error to find available inventory quantity: shopId: {}, caused: {]", shopId, getRes.getError());
            return null;
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
        if (channelStock < 0L) {
            log.warn("shop(id={}) channelStock is less than 0 for sku(code={}), current channelStock is:{}, shareStock is:{}",
                    shopId, skuCode, channelStock, shareStock);

            channelStock = 0L;
        }

        if (null != shopStockRule.getSafeStock()) {
            shareStock = shareStock - shopStockRule.getSafeStock();
        }

        //按照设定的比例确定推送数量
        stock = Math.max(0,
                channelStock
                        + shareStock * shopStockRule.getRatio() / 100
                        + (null == shopStockRule.getJitStock() ? 0 : shopStockRule.getJitStock())
        );
        log.info("after calculate, push stock quantity (skuCode is {},shopId is {}), is {}",
                skuCode, shopId, stock);
        return stock;
    }

    /**
     * @param warehouseIds
     * @return
     * @Description 校验门店类型为下单门店或营业状态为歇业，如果则门店店仓不可用；默认返回是false
     * @Date 2018/7/7
     */
    public List<Long> getAvailableForShopWarehouse(List<Long> warehouseIds) {

        List<Long> availableWarehouse = new ArrayList<>();
        for (Long warehouseId : warehouseIds) {
            try {
                WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
                if (warehouse.getWarehouseSubType() == null) {
                    continue;
                }
                //校验是否是店仓
                if (!Objects.equals(warehouse.getWarehouseSubType(), WarehouseType.SHOP_WAREHOUSE.value())) {
                    availableWarehouse.add(warehouseId);
                    continue;
                }

                //如果类型为下单门店或营业时间为歇业则不累加此仓库可用库存
                Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(), Long.parseLong(warehouse.getCompanyId()));
                if (shop == null) {
                    log.error("failed to find shop for outCode({}) and companyId({})",
                            warehouse.getOutCode(), warehouse.getCompanyId());
                    continue;
                }
                //如果是下单门店 返回true
                if (Objects.equals(ShopType.ORDERS_SHOP.value(), shop.getType())) {
                    continue;
                }
                //如果营业时间为歇业返回true
                if (Objects.equals(ShopOpeningStatus.CLOSING.value(), (ShopExtraInfo.fromJson(shop.getExtra()).getShopBusinessTime().getOpeningStatus()))) {
                    continue;
                }
                availableWarehouse.add(warehouseId);
            } catch (ServiceException e) {
                log.error("failed to find shop type and businessInfo for warehouse (id:{}),case:{}",
                        warehouseId, e.getMessage());
                continue;
            } catch (Exception e) {
                log.error("failed to find shop type and businessInfo for warehouse (id:{}),case:{}",
                        warehouseId, Throwables.getStackTraceAsString(e));
                continue;
            }
        }
        return availableWarehouse;
    }

    /**
     * @Description 查询云聚jit店铺
     * @Date        2018/9/13
     * @param
     * @return
     */
    //todo 缓存优化
    public List<OpenShop>  getYjShop() {

        Response<List<OpenShop>> resp = openShopReadService.findByChannel(MiddleChannel.YUNJUJIT.getValue());
        if(!resp.isSuccess()){
            log.error("can not find yunju jit shop");
        }
        return resp.getResult();
    }

    /*
     * @Description 查询云聚Jit订单占用的库存
     * @Date        2018/9/14
     * @param       shopId
     * @param       skuWareshouseIds
     * @return
     */
    public Table<String,Long,Long> getYjJitOccupyQty(Long shopId,
                                                     Map<String,List<Long>> skuWareshouseIds){
        Table<String,Long,Long> occupyTable = HashBasedTable.create();

        List<Long> shopIds = Arrays.asList(shopId);
        List<String> skuCodes =  Lists.newArrayList(skuWareshouseIds.keySet());
        List<Long> warehouseIds = Lists.newArrayList();
        skuWareshouseIds.values().forEach(item -> warehouseIds.addAll(item));
        List<SkuOrderLockStock> skuOrderLockStocks= Lists.newArrayList();

        if(CollectionUtils.isEmpty(shopIds) || CollectionUtils.isEmpty(skuCodes) || CollectionUtils.isEmpty(warehouseIds)){
            return occupyTable;
        }

        Response<List<SkuOrderLockStock>> orderResp = middleOrderReadService.findOccupyQuantityList(shopIds,warehouseIds,skuCodes);
        if(!orderResp.isSuccess()){
            log.error("failed to search Jit order for shopIds ({}) warehouseIds ({}) skuCodes ({}), cause:{}",shopIds,warehouseIds,skuCodes,orderResp.getError());
        }else{
            orderResp.getResult();
        }

        ShipmentItemCriteria criteria = new ShipmentItemCriteria();
        criteria.setShopIds(shopIds);
        criteria.setSkuCodes(skuCodes);
        criteria.setWarehouseIds(warehouseIds);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.findShipmentItems(criteria);

        skuWareshouseIds.forEach((skuCode,wareIds) -> {
            wareIds.forEach(wareId -> {
                List<Integer> qtys = Lists.newArrayList();
                skuOrderLockStocks.forEach(skuOrderLockStock -> {
                    if(Objects.equals(skuCode, skuOrderLockStock.getSkuCode()) && Objects.equals(wareId,skuOrderLockStock.getWarehouseId())){
                        qtys.add(skuOrderLockStock.getQuantity());
                    }
                });
                shipmentItems.forEach(shipmentItem -> {
                    if(Objects.equals(skuCode, shipmentItem.getSkuCode()) && Objects.equals(wareId,shipmentItem.getWarehouseId())){
                        qtys.add(shipmentItem.getQuantity());
                    }
                });

                if(CollectionUtils.isEmpty(qtys)){
                    return;
                }
                occupyTable.put(skuCode,wareId,new Long(qtys.stream().mapToInt(Integer::intValue).sum()));
            });
        });
        return occupyTable;
    }
}