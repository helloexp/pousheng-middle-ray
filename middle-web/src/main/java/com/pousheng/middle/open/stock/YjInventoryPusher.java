package com.pousheng.middle.open.stock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 云聚JIT库存推送优化版
 *
 * @author tanlongjun
 */
@Component
@Slf4j
public class YjInventoryPusher {
    @Autowired
    WarehouseCacher warehouseCacher;

    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;

    private Map<Long, OpenShop> jitShops = Maps.newHashMap();
    private Map<Long, OpenShop> bbcShops = Maps.newHashMap();

    @Autowired
    private YjStockPusher yjStockPusher;

    private static final JsonMapper MAPPER = JsonMapper.JSON_NON_EMPTY_MAPPER;
    @Autowired
    private StockPusherLogic stockPushLogic;

    /**
     * 批量推送
     *
     * @param changeDTOList
     */
    public void push(List<InventoryChangeDTO> changeDTOList) {
        if (CollectionUtils.isEmpty(changeDTOList)) {
            log.warn("empty inventory change list.skip to push.");
            return;
        }

        //构造JIT店铺Map
        if (jitShops.isEmpty() || bbcShops.isEmpty()) {
            try {
                List<OpenShop> jitShopList = stockPushLogic.getShopByChannel(MiddleChannel.YUNJUJIT.getValue());
                List<OpenShop> bbcShopList = stockPushLogic.getShopByChannel(MiddleChannel.YUNJUBBC.getValue());
                jitShopList.forEach(openShop -> {
                    jitShops.put(openShop.getId(), openShop);
                });
                bbcShopList.forEach(openShop -> {
                    bbcShops.put(openShop.getId(), openShop);
                });
            } catch (Exception e) {
                log.error("failed init query all jit open shop list.", e);
                return;
            }

        }
        changeDTOList.forEach(this::push);

    }

    /**
     * 单个推送
     * InventoryChangeDTO数据格式:
     * 1.只有skuCode
     * 2.只有skuCode和warehouseId
     * 3.只有skuCode和shopId
     *
     * @param dto
     */
    public void push(InventoryChangeDTO dto) {
        if (Objects.isNull(dto)) {
            log.warn("param is null.skip push.");
            return;
        }
        if (StringUtils.isBlank(dto.getSkuCode())) {
            log.warn("param skuCode is empty.skip push.");
            return;
        }

        List<String> skuCodes = Lists.newArrayList(dto.getSkuCode());
        Long shopId = null;
        final List<Long> warehouseIds;
        // 有warehouseId和SkuCode的情况
        if (!Objects.isNull(dto.getWarehouseId())) {
            warehouseIds = Lists.newArrayList(dto.getWarehouseId());
            //找到对应的店铺id, 这些店铺需要进行库存推送
            Response<List<Long>> response = warehouseShopRuleClient.findShopIdsByWarehouseId(
                dto.getWarehouseId());
            if (!response.isSuccess()
                || Objects.isNull(response.getResult())) {
                log.error("failed to find out shops for warehouse(id={}), error code:{}",
                    dto.getWarehouseId(), response.getError());
                return;
            }
            response.getResult().forEach(openShopId -> {
                if (jitShops.containsKey(openShopId) || bbcShops.containsKey(openShopId)) {
                    handle(openShopId, skuCodes, warehouseIds);
                }
            });
        } else if (!Objects.isNull(dto.getShopId())) {
            shopId = dto.getShopId();
            if (jitShops.containsKey(shopId) || bbcShops.containsKey(shopId)) {
                warehouseIds = stockPushLogic.getWarehouseIdsByShopId(shopId);
                log.debug("yunju jit/bbc inventory push warehouseIds:{}", warehouseIds.toString());
                handle(shopId, skuCodes, warehouseIds);
            }
        } else {
            //仓库为空且店铺为空的情况 在YjJitStockPusher里处理。外层已经条件过滤了。不会执行到这里。故此处不处理
            log.warn("skip to handle only include skuCode info.param:{}", MAPPER.toJson(dto));
        }
    }

    private void handle(Long shopId, List<String> skuCodes, List<Long> warehouseIds) {
        if (jitShops.containsKey(shopId)) {
            Table<String, Long, Long> stocks = collectStocks(shopId, skuCodes, warehouseIds);
            log.debug("yunju jit inventory push stocks:{}", stocks == null ? null : stocks.toString());
            //调用云聚接口推送库存
            yjStockPusher.send(shopId, stocks, null);
        }
        if (bbcShops.containsKey(shopId)) {
            Table<String, Long, Long> stocks = collectStocks(shopId, skuCodes, warehouseIds);
            log.debug("yunju bbc inventory push stocks:{}", stocks == null ? null : stocks.toString());
            String visualWarehouseCode = bbcShops.get(shopId).getExtra().get(TradeConstants.VISUAL_WAREHOUSE_CODE);
            //调用云聚接口推送库存
            yjStockPusher.send(shopId, stocks, visualWarehouseCode);
        }
    }

    /**
     * 处理
     *
     * @param shopId
     * @param skuCodes
     * @param warehouseIds
     */
    private Table<String, Long, Long> collectStocks(Long shopId, List<String> skuCodes, List<Long> warehouseIds) {
        //店铺商品分组
        List<String> filteredSkuCodes = stockPushLogic.filterShopSkuGroup(shopId, skuCodes);
        log.debug("yunju inventory push filteredSkuCodes:{}", filteredSkuCodes == null ? null : filteredSkuCodes.toString());
        //店铺库存推送规则是否启用
        ShopStockRule shopStockRule = warehouseShopRuleClient.findByShopId(shopId);
        if(shopStockRule.getStatus() < 0){
            log.warn("there is no valid stock push rule for shop(id={}), so skip to continue", shopId);
            return null;
        }
        //库存推送规则
        Map<String, ShopStockRuleDto> warehouseShopStockRules = stockPushLogic.getWarehouseShopStockRules(shopId,
            filteredSkuCodes);
        log.debug("yunju inventory push warehouseShopStockRules:{}",
            warehouseShopStockRules == null ? null : warehouseShopStockRules.toString());
        //过滤仓库商品分组
        Map<String, List<Long>> skuWareshouseIds = stockPushLogic.filterWarehouseSkuGroup(shopId, filteredSkuCodes,
            warehouseIds);
        log.debug("yunju inventory push skuWareshouseIds:{}",
            skuWareshouseIds == null ? null : skuWareshouseIds.toString());
        //计算可用可用库存
        return yjStockPusher.calculate(shopId, skuWareshouseIds, warehouseShopStockRules);
    }
}
