package com.pousheng.middle.open.stock;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.web.item.cacher.ItemMappingCacher;
import com.pousheng.middle.web.item.component.CalculateRatioComponent;
import com.pousheng.middle.web.order.component.ShopMaxOrderLogic;
import com.pousheng.middle.web.yintai.component.MiddleYintaiItemService;
import com.pousheng.middle.web.yintai.component.MiddleYintaiShopService;
import com.pousheng.middle.web.yintai.dto.YintaiShop;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Autowired
    private ItemMappingCacher itemMappingCacher;
    @Autowired
    private CalculateRatioComponent calculateRatioComponent;

    @Autowired
    private ShopMaxOrderLogic shopMaxOrderLogic;
    @Autowired
    private OpenShopReadService openShopReadService;
    @Autowired
    private MiddleYintaiShopService middleYintaiShopService;
    @Autowired
    private MiddleYintaiItemService middleYintaiItemService;
    @Autowired
    private InventoryClient inventoryClient;

    public void push(List<String> skuCodes) {

        if (log.isDebugEnabled()) {
            log.debug("STOCK-PUSHER-SUBMIT-START param: skuCodes:{},start time:{}", skuCodes, System.currentTimeMillis());
        }
        log.info("start to push skus: {}", skuCodes);

        Table<Long, String, Integer> vipSkuStock = HashBasedTable.create();

        //库存推送日志记录
        final List<StockPushLog> thirdStockPushLogs = new CopyOnWriteArrayList<>();
        List<StockPushLog> logs = Lists.newArrayList();
        for (String skuCode : skuCodes) {
            try {
                //根据商品映射查询店铺
                //找到对应的店铺id, 这些店铺需要进行库存推送
                List<ItemMapping> itemMappings = itemMappingCacher.findBySkuCode(skuCode);
                if (CollectionUtils.isEmpty(itemMappings)) {
                    if (middleYintaiItemService.stockAdjustHandle(skuCode)) {
                        continue;
                    }
                    log.error("failed to find out shops by skuCode={}, error code:{}", skuCode);
                    continue;
                }
                //计算库存分配并将库存推送到每个外部店铺去

                List<Long> shopIds = itemMappings.stream()
                        .filter(itemMapping -> MiddleChannel.from(itemMapping.getChannel()) != MiddleChannel.YINTAI)
                        .map(ItemMapping::getOpenShopId).collect(Collectors.toList());
                //银泰店铺展开
                itemMappings.stream()
                        .filter(itemMapping -> MiddleChannel.from(itemMapping.getChannel()) == MiddleChannel.YINTAI)
                        .findFirst().ifPresent(itemMapping -> {
                    Response<List<OpenShop>> yintaiShops = openShopReadService.findByChannel(MiddleChannel.YINTAI.getValue());
                    if (yintaiShops.isSuccess()) {
                        shopIds.addAll(yintaiShops.getResult().stream().filter(openShop -> openShop.getStatus() > 0).map(OpenShop::getId).collect(Collectors.toList()));
                    }
                });
                log.info("find shopIds({}) by skuCode({})",shopIds.toString(),skuCode);

                handle(shopIds,skuCode,logs);
                //尝试创建银泰映射
                if (itemMappings.stream().noneMatch(mapping -> MiddleChannel.from(mapping.getChannel()) == MiddleChannel.YINTAI)) {
                    middleYintaiItemService.stockAdjustHandle(skuCode);
                }
            } catch (Exception e) {
                log.error("failed to push stock,sku is {}", skuCode);
            }
        }

        ////官网批量推送
        //if(!paranaSkuStock.isEmpty()) {
        //    stockPushLogic.sendToParana(paranaSkuStock);
        //}
        if (log.isDebugEnabled()) {
            log.debug("STOCK-PUSHER-SUBMIT-END param: skuCodes:{},end time:{}", skuCodes, System.currentTimeMillis());
        }
    }

    /**
     * 抽离处理推送逻辑
     * @param shopIds 店铺列表
     * @param skuCode
     * @param logs
     */
    public void handle(List<Long> shopIds, String skuCode,
                       List<StockPushLog> logs) {
        for (Long shopId : shopIds) {
            CompletableFuture.runAsync(() -> {
                Table<Long, String, Integer> paranaSkuStock = HashBasedTable.create();
                log.info("start to push sku to shopId: {},skuCode:{}", shopId, skuCode);
                Long stock = 0L;
                try {
                    OpenShop openShop = stockPushLogic.openShopCacher.getUnchecked(shopId);
                    if (openShop == null) {
                        log.error("failed to find shop(id={})，so skip to continue", shopId);
                        return;
                    }

                    if (Objects.equals(shopId, mposOpenShopId)) {
                        return;
                    }

                    // mpos的店铺不推送
                    if (openShop.getShopName().startsWith("mpos")) {
                        return;
                    }

                    //判断当前skuCode是否在当前店铺卖，如果不卖则跳过
                    List<ItemMapping> itemMappings = itemMappingCacher.findBySkuCodeAndShopId(skuCode, shopId);
                    //Collections.sort(itemMappings, (ItemMapping itemMapping1, ItemMapping itemMapping2)->itemMapping2.getCreatedAt().compareTo(itemMapping1.getCreatedAt()));
                    if (CollectionUtils.isEmpty(itemMappings)) {
                        log.warn("item mapping not found by skuCode={},openShopId={}", skuCode, shopId);
                        return;
                    }
                    //log.info("find itemMappings({}) by skuCode({}) and shopId({})",itemMappings.toString(),skuCode,shopId);
                    Response<ShopStockRuleDto> rShopStockRule = warehouseShopRuleClient.findByShopIdAndSku(shopId,
                        skuCode);
                    if (!rShopStockRule.isSuccess()) {
                        log.warn("failed to find shop stock push rule for shop(id={}), error code:{}",
                            shopId, rShopStockRule.getError());
                        return;
                    }
                    //和安全库存进行比较, 确定推送库存数量
                    ShopStockRule shopStockRule = rShopStockRule.getResult().getShopRule();
                    log.info("shopStockRule result is:{}",shopStockRule);
                    if (shopStockRule.getStatus() < 0) { //非启用状态
                        log.warn("there is no valid stock push rule for shop(id={}), so skip to continue", shopId);
                        return;
                    }

                    boolean isYintai = MiddleChannel.YINTAI == MiddleChannel.from(openShop.getChannel());
                    if (isYintai) {//银泰每个店柜推送库存只看自身店仓
                        yintaiHandle(rShopStockRule.getResult(), skuCode, itemMappings, openShop);
                        return;
                    }
                    //计算每个店铺的可用库存
                    Response<List<Long>> rWarehouseIds = warehouseRulesClient.findWarehouseIdsByShopId(shopId);
                    if (!rWarehouseIds.isSuccess()) {
                        log.error("find warehouse list by shopId fail: shopId: {}, caused: {]", shopId,
                            rWarehouseIds.getError());
                        return;
                    }

                        //根据商品分组规则判断该店铺是否运行售卖此SKU
                        boolean isOnSale = queryHkWarhouseOrShopStockApi.isVendible(skuCode, shopId);
                        //根据商品分组规则，如果不售卖则推送0
                        if (!isOnSale) {
                            log.info(
                                    "this sku is not on sale in this shop, so set push stock to 0 (skuCode is {},shopId is {})",
                                    skuCode, shopId);
                            stock = 0L;
                        } else {
                            //跟店铺类型、营业状态过滤可用店仓
                            List<Long> warehouseIds = stockPushLogic.getAvailableForShopWarehouse(
                                    rWarehouseIds.getResult());
                            //根据商品分组规则过滤可发货的仓库列表
                            String companyCode = openShop.getExtra().get("companyCode");
                            if (companyCode == null || "".equals(companyCode)) {
                                log.error("find open shop companyCode fail: shopId: {}, so skip to continue", shopId);
                                return;
                            }
                            warehouseIds = queryHkWarhouseOrShopStockApi.isVendibleWarehouse(skuCode, warehouseIds,
                                    companyCode);

                            //过滤超过最大接单量的店仓
                            warehouseIds = shopMaxOrderLogic.filterWarehouse(warehouseIds);

                            if (warehouseIds == null || warehouseIds.isEmpty()) {
                                stock = 0L;
                            } else {
                                stock = stockPushLogic.calculateStock(shopId, skuCode, warehouseIds,
                                        rShopStockRule.getResult());
                            }

                        }
                        if (stock == null) {
                            return;
                        }

                    log.info("after calculate, push stock quantity (skuCode is {},shopId is {}), is {}",
                        skuCode, shopId, stock);

                    //判断店铺是否是官网的
                    if (Objects.equals(openShop.getChannel(), MiddleChannel.OFFICIAL.getValue())) {
                        log.info("start to push to official shop: {} sku code:{} with quantity: {}", openShop,skuCode,stock);
                        paranaSkuStock.put(shopId, skuCode, Math.toIntExact(stock));
                    } else {
                        log.info("start to push to third part shop: {} sku code:{} with origin quantity: {}", openShop, skuCode,stock);
                        //库存推送-----第三方只支持单笔更新库存,使用线程池并行处理
                        // 如果只有1条，或者多条都没有设置比例，就按默认的推第一个
                    /*    List<ItemMapping> ratioItemMappings = itemMappings.stream().filter(
                            im -> Objects.nonNull(im.getRatio())).collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(ratioItemMappings)) {
                            ItemMapping itemMapping = itemMappings.get(0);
                            stockPushLogic.prallelUpdateStock(itemMapping, stock,openShop);
                        } else {
                            // 设置比例按比例推，未设置的不推
                            for (ItemMapping im : ratioItemMappings) {
                                stockPushLogic.prallelUpdateStock(im, stock * im.getRatio() / HUNDRED,openShop);
                            }
                        }*/

                        for (ItemMapping im : itemMappings) {
                            try {
                                Integer ratio = calculateRatioComponent.getRatio(im,shopStockRule);
                                if (Arguments.notNull(ratio)){
                                    stockPushLogic.prallelUpdateStock(im, stock * ratio / HUNDRED,openShop);
                                } else {
                                    log.warn("skip to push to third part shop: {} sku code:{} with origin quantity: {},because ratio is null", openShop, skuCode,stock);
                                }
                            } catch (Exception e) {
                                log.error("failed to push stock of itemmapping({}) to shop(id={}), cause: {}", im, shopId, Throwables.getStackTraceAsString(e));
                            }
                        }
                        log.info("parall update stock return");
                    }
                } catch (Exception e) {
                    log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                        skuCode, shopId, Throwables.getStackTraceAsString(e));
                    stockPushLogic.createAndPushLogs(logs, skuCode, shopId, null, stock, Boolean.FALSE, e.getMessage());
                }
                //官网推送
                if(!paranaSkuStock.isEmpty()) {
                    stockPushLogic.sendToParana(paranaSkuStock);
                }
            },stockPushLogic.executorService);
        }
    }

    private void yintaiHandle(ShopStockRuleDto dto, String skuCode, List<ItemMapping> itemMappings,OpenShop openShop) {
        ShopStockRule shopRule = dto.getShopRule();//门店规则
        //未开启的仓库发货设置拿不到，这里改用默认发货仓下的仓库列表做是否可推送库存控制
        Map<Long, ShopStockRule> warehouseRule = dto.getWarehouseRule();//店仓规则，只有启用的
        Response<List<Long>> warehouseIdsResp = warehouseRulesClient.findWarehouseIdsByShopId(openShop.getId());
        Map</*店柜Id*/String, /*店仓ids*/List<Long>> counterWarehouseMapping = Maps.newHashMap();//基本上是一对一，这里支持一个店柜对应多个店仓

        if (!warehouseIdsResp.isSuccess()) {
            log.error("fail to find shop stock rule , shopId({}), error:({})", openShop.getId(), warehouseIdsResp.getError());
            return;
        }
        if (CollectionUtils.isEmpty(warehouseIdsResp.getResult())) {
            log.info("warehouseIds is empty, shopId:({})", openShop.getId());
            return;
        }
        Set<Long> warehouseIds = Sets.newHashSet(warehouseIdsResp.getResult());

        //获取有映射关系的店仓列表
        List<WarehouseDTO> shopWarehouses = middleYintaiShopService.getShopWarehouses(shopRule.getShopId());
        for (WarehouseDTO shopWarehouse : shopWarehouses) {
            if (!warehouseIds.contains(shopWarehouse.getId())) {//默认发货仓里没有的店仓不推送
                log.info("warehouse not in shop stock rule list, skip. warehouse:({})", shopWarehouse.getWarehouseCode());
                continue;
            }
//            ShopStockRule shopStockRule = warehouseRule.get(shopWarehouse.getId());
//            if (shopStockRule!= null && shopStockRule.getStatus() != 1) {
//                log.info("shopStockRule warehouse status not enable, skip. warehouse:({})", shopWarehouse.getWarehouseCode());
//                continue;
//            }
            List<Long> wids = MoreObjects.firstNonNull(counterWarehouseMapping.get(shopWarehouse.getCounterId()), Lists.newArrayList());
            wids.add(shopWarehouse.getId());
            counterWarehouseMapping.put(shopWarehouse.getCounterId(), wids);
        }

        if (counterWarehouseMapping.isEmpty()) {
            log.info("not available counter and warehouse list.");
            return;
        }

        List<YintaiShop> counterList = middleYintaiShopService.getShopList();
        Map<String, String> counterMapping = counterList.stream().filter(counter -> !Strings.isNullOrEmpty(counter.getChannelShopId())).collect(Collectors.toMap(YintaiShop::getChannelShopId, YintaiShop::getShopName, (a, b) -> a));
        for (Map.Entry<String, List<Long>> entry : counterWarehouseMapping.entrySet()) {
            log.info("calculate stock by counterId({}), warehouseIds({})",entry.getKey(), entry.getValue());
            boolean flag = false;
            for (Long warehouseId : entry.getValue()) {
                Response<Optional<InventoryDTO>> inventory = inventoryClient.findInventory(warehouseId, skuCode);
                if (inventory.isSuccess() && inventory.getResult().isPresent()) {
                    flag = true;
                }
            }
            if (!flag) {
                log.info("inventory not exist, skuCode({}), warehouseId:({})", skuCode, entry.getValue());
                continue;
            }

            Long quantity = stockPushLogic.calculateStock(shopRule.getShopId(), skuCode, entry.getValue(), dto);
            log.info("calculate stock is ({})", quantity);
            for (ItemMapping itemMapping : itemMappings) {
                try {
                    ItemMapping copyMapping = convert(itemMapping);//数据来自缓存
                    //这里没有计算映射纬度上的推送比例
                    copyMapping.setOpenShopId(Long.valueOf(entry.getKey()));//换成店柜id
                    copyMapping.setOpenShopName(counterMapping.get(entry.getKey()));
                    stockPushLogic.prallelUpdateStock(copyMapping, quantity, openShop);
                } catch (Exception e) {
                    log.error("failed to push stock of itemmapping({}) to counterId(id={}), cause: {}", itemMapping, entry.getKey(), Throwables.getStackTraceAsString(e));
                }
            }
        }

    }

    private ItemMapping convert(ItemMapping itemMapping) {
        ItemMapping copyMapping = new ItemMapping();
        copyMapping.setId(itemMapping.getId());
        copyMapping.setChannel(itemMapping.getChannel());
        copyMapping.setOpenShopId(itemMapping.getOpenShopId());
        copyMapping.setOpenShopName(itemMapping.getOpenShopName());
        copyMapping.setItemId(itemMapping.getItemId());
        copyMapping.setItemName(itemMapping.getItemName());
        copyMapping.setChannelItemId(itemMapping.getChannelItemId());
        copyMapping.setSkuCode(itemMapping.getSkuCode());
        copyMapping.setChannelSkuId(itemMapping.getChannelSkuId());
        copyMapping.setSkuAttributes(itemMapping.getSkuAttributes());
        copyMapping.setProperties(itemMapping.getProperties());
        copyMapping.setStatus(itemMapping.getStatus());
        copyMapping.setRatio(itemMapping.getRatio());
        return copyMapping;
    }


}
