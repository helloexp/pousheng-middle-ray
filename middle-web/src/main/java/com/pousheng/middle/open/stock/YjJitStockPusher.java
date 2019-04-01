package com.pousheng.middle.open.stock;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.open.stock.yunju.dto.YjStockInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/8/31
 */
@Component
@Slf4j
public class YjJitStockPusher {
    @Autowired
    WarehouseCacher warehouseCacher;
    @Setter
    @Value("${stock.push.cache.enable: true}")
    private boolean StockPusherCacheEnable;
    @Autowired
    private StockPushCacher stockPushCacher;
    @Autowired
    private StockPusherLogic stockPushLogic;

    public void push(List<String> skuCodes) {
        stockPushLogic.getYjShop().forEach(openShop -> {
            Long shopId = openShop.getId();
            //店铺默认发货仓
            List<Long> warehouseIds = stockPushLogic.getWarehouseIdsByShopId(shopId);
            if (log.isDebugEnabled()) {
                log.debug("yunju jit stock push warehouseIds:{}", warehouseIds.toString());
            }
            //店铺商品分组
            List<String> filteredSkuCodes = stockPushLogic.filterShopSkuGroup(shopId, skuCodes);
            if (log.isDebugEnabled()) {
                log.debug("yunju jit stock push filteredSkuCodes:{}", filteredSkuCodes == null ? null : filteredSkuCodes.toString());
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
            Table<String, Long, Long> stocks = this.calculate(shopId, skuWareshouseIds, warehouseShopStockRules);
            if (log.isDebugEnabled()) {
                log.debug( "yunju jit stock push stocks:{}", stocks == null ? null : stocks.toString());
            }
            //调用云聚接口推送库存
            this.send(shopId, stocks);
        });
    }

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

    public void send(Long shopId, Table<String, Long, Long> stocks) {
        List<YjStockInfo> yjStockInfos = Lists.newArrayList();
        stockPushLogic.appendYjRequest(yjStockInfos, stocks);
        //云聚Jit库存更新接口最大接受500条
        int size = yjStockInfos.size();
        int singleMax = 500;
        int fromIdx = 0;
        int toIdx = size < singleMax ? size : singleMax;
        while (fromIdx < toIdx) {
            stockPushLogic.sendToYj(shopId, yjStockInfos.subList(fromIdx, toIdx));
            fromIdx = toIdx;
            toIdx = size - toIdx < singleMax ? toIdx + (size - toIdx) % singleMax : toIdx + singleMax;
        }
    }

}
