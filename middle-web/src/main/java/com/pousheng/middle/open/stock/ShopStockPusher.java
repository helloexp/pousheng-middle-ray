package com.pousheng.middle.open.stock;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-14
 */
@Component
@Slf4j
// TODO 推送优化改造
public class ShopStockPusher {

    @Autowired
    private StockPusherLogic stockPushLogic;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;
    private static final Integer HUNDRED = 100;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @Value("${terminus.rocketmq.stockLogTopic}")
    private String stockLogTopic;

    @Autowired
    private StockPushCacher stockPushCacher;
    @Setter
    @Value("${stock.push.cache.enable: true}")
    private boolean StockPusherCacheEnable;

    @RpcConsumer
    private MappingReadService mappingReadService;

    public void push(List<String> skuCodes) {

        if (log.isDebugEnabled()) {
            log.debug("STOCK-PUSHER-SUBMIT-START param: skuCodes:{},start time:{}", skuCodes, System.currentTimeMillis());
        }
        log.info("start to push skus: {}", skuCodes);
        Table<Long, String, Integer> shopSkuStock = HashBasedTable.create();
        //库存推送日志记录
        final List<StockPushLog> thirdStockPushLogs = new CopyOnWriteArrayList<>();
        List<StockPushLog> logs = Lists.newArrayList();
        for (String skuCode : skuCodes) {
            try {
                Long spuId = stockPushLogic.skuCodeCacher.getUnchecked(skuCode);
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
                    Long stock = 0L;
                    try {
                        OpenShop openShop = stockPushLogic.openShopCacher.getUnchecked(shopId);
                        if (openShop == null) {
                            log.error("failed to find shop(id={})，so skip to continue", shopId);
                            continue;
                        }

                        if (Objects.equals(shopId, mposOpenShopId)) {
                            continue;
                        }

                        //判断当前skuCode是否在当前店铺卖，如果不卖则跳过
                        Response<List<ItemMapping>> itemMappingRes = mappingReadService.listBySkuCodeAndOpenShopId(skuCode, shopId);
                        if (!itemMappingRes.isSuccess()) {
                            log.error("fail to find item mapping by skuCode={},openShopId={},cause:{}",
                                    skuCode, shopId, itemMappingRes.getError());
                            continue;
                        }
                        List<ItemMapping> itemMappings = itemMappingRes.getResult();
                        if (CollectionUtils.isEmpty(itemMappings)) {
                            log.warn("item mapping not found by skuCode={},openShopId={}", skuCode, shopId);
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
                            log.warn("there is no valid stock push rule for shop(id={}), so skip to continue", shopId);
                            continue;
                        }

                        //计算每个店铺的可用库存
                        Response<List<Long>> rWarehouseIds = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
                        if (!rWarehouseIds.isSuccess()) {
                            log.error("find warehouse list by shopId fail: shopId: {}, caused: {]", shopId, rWarehouseIds.getError());
                            continue;
                        }

                        //根据商品分组规则判断该店铺是否运行售卖此SKU
                        boolean isOnSale = queryHkWarhouseOrShopStockApi.isVendible(skuCode, shopId);
                        //根据商品分组规则，如果不售卖则推送0
                        if (!isOnSale) {
                            log.info("this sku is not on sale in this shop, so set push stock to 0 (skuCode is {},shopId is {})", skuCode, shopId);
                            stock = 0L;
                        } else {
                            //跟店铺类型、营业状态过滤可用店仓
                            List<Long> warehouseIds = stockPushLogic.getAvailableForShopWarehouse(rWarehouseIds.getResult());
                            //根据商品分组规则过滤可发货的仓库列表
                            String companyCode = openShop.getExtra().get("companyCode");
                            if (companyCode == null || "".equals(companyCode)) {
                                log.error("find open shop companyCode fail: shopId: {}, so skip to continue", shopId);
                                continue;
                            }
                            warehouseIds = queryHkWarhouseOrShopStockApi.isVendibleWarehouse(skuCode, warehouseIds, companyCode);

                            if (warehouseIds == null || warehouseIds.isEmpty()) {
                                stock = 0L;
                            } else {
                                stock = stockPushLogic.calculateStock(shopId, skuCode, warehouseIds, shopStockRule);
                            }

                            //校验缓存中是否有推送记录且推送数量一致，则本次不推送
                            if(StockPusherCacheEnable){
                                Integer cacheStock = stockPushCacher.getFromRedis(StockPushCacher.ORG_TYPE_SHOP, shopId.toString(), skuCode);
                                if(log.isDebugEnabled()){
                                    log.debug("compare current stock({}) with cacheStock({}),result is {}", stock, cacheStock, (!Objects.isNull(cacheStock) && stock.intValue() == cacheStock.intValue()));
                                }
                                if(!Objects.isNull(cacheStock) && stock.intValue() == cacheStock.intValue()){
                                    continue;
                                }
                            }
                        }
                        if (stock == null) {
                            continue;
                        }

                        log.info("after calculate, push stock quantity (skuCode is {},shopId is {}), is {}",
                                skuCode, shopId, stock);

                        //判断店铺是否是官网的
                        if (Objects.equals(openShop.getChannel(), MiddleChannel.OFFICIAL.getValue())) {
                            log.info("start to push to official shop: {}, with quantity: {}", openShop, stock);
                            shopSkuStock.put(shopId, skuCode, Math.toIntExact(stock));
                        } else {
                            log.info("start to push to third part shop: {}, with quantity: {}", openShop, stock);
                            //库存推送-----第三方只支持单笔更新库存,使用线程池并行处理
                            log.info("parall update stock start");
                            // 如果只有1条，或者多条都没有设置比例，就按默认的推第一个
                            List<ItemMapping> ratioItemMappings = itemMappings.stream().filter(im -> Objects.nonNull(im.getRatio())).collect(Collectors.toList());
                            if (CollectionUtils.isEmpty(ratioItemMappings)) {
                                ItemMapping itemMapping = itemMappings.get(0);
                                stockPushLogic.prallelUpdateStock(itemMapping, stock);
                            } else {
                                // 设置比例按比例推，未设置的不推
                                for (ItemMapping im : ratioItemMappings) {
                                    stockPushLogic.prallelUpdateStock(im, stock * im.getRatio() / HUNDRED);
                                }
                            }
                            log.info("parall update stock return");
                        }
                    } catch (Exception e) {
                        log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                                skuCode, shopId, Throwables.getStackTraceAsString(e));
                        stockPushLogic.createAndPushLogs(logs, skuCode, shopId, null, stock, Boolean.FALSE, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("failed to push stock,sku is {}", skuCode);
            }
        }

        //官网批量推送
        stockPushLogic.sendToParana(shopSkuStock);

        if (log.isDebugEnabled()) {
            log.debug("STOCK-PUSHER-SUBMIT-END param: skuCodes:{},end time:{}", skuCodes, System.currentTimeMillis());
        }
    }

}
