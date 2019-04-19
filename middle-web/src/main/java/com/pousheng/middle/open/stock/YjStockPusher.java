package com.pousheng.middle.open.stock;

import com.google.common.collect.*;
import com.pousheng.middle.open.stock.yunju.dto.YjStockInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/8/31
 */
@Component
@Slf4j
public class YjStockPusher {
    @Autowired
    WarehouseCacher warehouseCacher;
    @Setter
    @Value("${stock.push.cache.enable: true}")
    private boolean StockPusherCacheEnable;
    @Autowired
    private StockPusherLogic stockPushLogic;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;

    public void push(List<String> skuCodes) {
        stockPushLogic.getShopByChannel(MiddleChannel.YUNJUJIT.getValue()).forEach(openShop -> {
            //计算可用可用库存
            Table<String, Long, Long> stocks = collectStocks(skuCodes, openShop);
            if (log.isDebugEnabled()) {
                log.debug( "yunju jit stock push stocks:{}", stocks == null ? null : stocks.toString());
            }
            //调用云聚接口推送库存
            this.send(openShop.getId(), stocks, null);
        });
        stockPushLogic.getShopByChannel(MiddleChannel.YUNJUBBC.getValue()).forEach(openShop -> {
            //计算可用可用库存
            Table<String, Long, Long> stocks = collectStocks(skuCodes, openShop);
            if (log.isDebugEnabled()) {
                log.debug( "yunju bbc stock push stocks:{}", stocks == null ? null : stocks.toString());
            }
            String visualWarehouseCode = openShop.getExtra().get(TradeConstants.VISUAL_WAREHOUSE_CODE);
            //调用云聚接口推送库存
            this.send(openShop.getId(), stocks, visualWarehouseCode);
        });
    }

    private Table<String, Long, Long> collectStocks(List<String> skuCodes, OpenShop openShop) {
        Long shopId = openShop.getId();
        //店铺默认发货仓
        List<Long> warehouseIds = stockPushLogic.getWarehouseIdsByShopId(shopId);
        if (log.isDebugEnabled()) {
            log.debug("yunju stock push warehouseIds:{}", warehouseIds.toString());
        }
        //店铺商品分组
        List<String> filteredSkuCodes = stockPushLogic.filterShopSkuGroup(shopId, skuCodes);
        if (log.isDebugEnabled()) {
            log.debug("yunju jit stock push filteredSkuCodes:{}", filteredSkuCodes == null ? null : filteredSkuCodes.toString());
        }
        //店铺库存推送规则是否启用
        ShopStockRule shopStockRule = warehouseShopRuleClient.findByShopId(shopId);
        if(shopStockRule.getStatus() < 0){
            log.warn("there is no valid stock push rule for shop(id={}), so skip to continue", shopId);
            return HashBasedTable.create();
        }

        //库存推送规则
        Map<String, ShopStockRuleDto> warehouseShopStockRules = stockPushLogic.getWarehouseShopStockRules(shopId, filteredSkuCodes);
        if (log.isDebugEnabled()) {
            log.debug("yunju jit stock push warehouseShopStockRules:{}", warehouseShopStockRules == null ? null : warehouseShopStockRules.toString());
        }

        //过滤仓库商品分组
        Map<String, List<Long>> skuWareshouseIds = stockPushLogic.filterWarehouseSkuGroup(shopId, filteredSkuCodes, warehouseIds);
        if (log.isDebugEnabled()) {
            log.debug("yunju jit stock push skuWareshouseIds:{}", skuWareshouseIds == null ? null : skuWareshouseIds.toString());
        }
        //计算可用可用库存
        return this.calculate(shopId, skuWareshouseIds, warehouseShopStockRules);
    }

    /**
     * 计算 skuCode、shopId 各个实仓可用库存
     * @param shopId
     * @param skuWareshouseIds
     * @param warehouseShopStockRules
     * @return
     */
    public Table<String, Long, Long> calculate(Long shopId, Map<String, List<Long>> skuWareshouseIds, Map<String, ShopStockRuleDto> warehouseShopStockRules) {
        //查询Jit占用库存 包括 查询Jit失效订单占用 和 查询Jit拣货单发货单占用
        //Table<String,Long,Long> occupyTable = stockPushLogic.getYjJitOccupyQty(shopId,skuWareshouseIds);

        //查询可用库存
        Table<String, Long, Long> stockTable = stockPushLogic.calculateYjStock(shopId, skuWareshouseIds, warehouseShopStockRules);

        //todo 2018-10-10 Jit库存推送数量原来是需要累加Jit占用库存的，现在去掉此逻辑，不需要再累加
        //累加可用库存和占用库存
        //occupyTable.cellSet().forEach(cell -> {
        //    if(stockTable.contains(cell.getRowKey(),cell.getColumnKey())){
        //        stockTable.put(cell.getRowKey(),cell.getColumnKey(),stockTable.get(cell.getRowKey(),cell.getColumnKey())+ cell.getValue());
        //    }
        //});

        //没有库存数量的sku、仓库 推送0
        skuWareshouseIds.forEach((skuCode, warehouseIds) -> {
            warehouseIds.forEach(warehouseId -> {
                if (!stockTable.contains(skuCode, warehouseId)) {
                    stockTable.put(skuCode, warehouseId, 0L);
                }
            });
        });

        return stockTable;
    }

    public void send(Long shopId, Table<String, Long, Long> stocks, String visualWarehouseCode) {
        List<YjStockInfo> yjStockInfos = Lists.newArrayList();
        // 老的逻辑不变，保证 redis 缓存逻辑按照实仓存储
        stockPushLogic.appendYjRequest(yjStockInfos, stocks);
        // 针对虚仓，记录 skuCode 到 yjStockInfo 列表的映射
        ArrayListMultimap<String, YjStockInfo> skuCode2YjStockInfos = ArrayListMultimap.create();
        //云聚Jit库存更新接口最大接受500条
        int singleMax = 500;
        if (StringUtils.isNotEmpty(visualWarehouseCode)) {
            yjStockInfos = real2Visual(yjStockInfos, skuCode2YjStockInfos, visualWarehouseCode);
        }
        List<List<YjStockInfo>> yjStockInfoPartition = Lists.partition(yjStockInfos, singleMax);
        for (List<YjStockInfo> yjStockInfoSubList : yjStockInfoPartition) {
            stockPushLogic.sendToYj(shopId, yjStockInfoSubList, skuCode2YjStockInfos);
        }
    }

    private List<YjStockInfo> real2Visual(List<YjStockInfo> yjStockInfos,
                             Multimap<String, YjStockInfo> skuCode2YjStockInfos, String visualWarehouseCode) {
        List<YjStockInfo> yjStockInfoSummary = Lists.newArrayList();
        Map<String, List<YjStockInfo>> groupBySkuCode =
                yjStockInfos.stream().collect(Collectors.groupingBy(YjStockInfo::getBarCode));
        int lineNo = 1;
        for (Map.Entry<String, List<YjStockInfo>> entry : groupBySkuCode.entrySet()) {
            int totalNum = entry.getValue().stream().mapToInt(YjStockInfo::getNum).sum();
            skuCode2YjStockInfos.putAll(entry.getKey(), entry.getValue());
            yjStockInfoSummary.add(YjStockInfo.builder()
                    .lineNo(String.valueOf(lineNo++))
                    .warehouseCode(visualWarehouseCode)
                    .barCode(entry.getKey())
                    .num(totalNum)
                    .build());
        }
        return yjStockInfoSummary;
    }
}
