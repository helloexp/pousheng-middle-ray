package com.pousheng.middle.open.stock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
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
public class YjJitInventoryPusher {
    @Autowired
    WarehouseCacher warehouseCacher;

    @Autowired
    private StockPushCacher stockPushCacher;

    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;

    private Map<Long, OpenShop> jitShops = Maps.newHashMap();

    @Autowired
    private YjJitStockPusher yjJitStockPusher;

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
        if (jitShops.isEmpty()) {
            List<OpenShop> shopList = Lists.newArrayList();
            try {
                shopList = stockPushLogic.getYjShop();
            } catch (Exception e) {
                log.error("failed init query all jit open shop list.", e);
                return;
            }
            shopList.forEach(openShop -> {
                jitShops.put(openShop.getId(), openShop);
            });
        }
        changeDTOList.forEach(dto -> {
            push(dto);
        });

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
                if (jitShops.containsKey(openShopId)) {
                    handle(openShopId, skuCodes, warehouseIds);
                }
            });
        } else if (!Objects.isNull(dto.getShopId())) {
            // 有shopId和SkuCode的情况
            shopId = dto.getShopId();
            // 若不是JIT店铺 则不处理
            if (!jitShops.containsKey(shopId)) {
                log.warn("the shop is not jit shop.skip to handle.shopId:{}", shopId);
                return;
            }
            warehouseIds = stockPushLogic.getWarehouseIdsByShopId(shopId);
            log.debug("yunju jit inventory push warehouseIds:{}", warehouseIds.toString());
            handle(shopId, skuCodes, warehouseIds);
        } else {
            //仓库为空且店铺为空的情况 在YjJitStockPusher里处理。外层已经条件过滤了。不会执行到这里。故此处不处理
            log.warn("skip to handle only include skuCode info.param:{}", MAPPER.toJson(dto));
        }
    }

    /**
     * 处理
     *
     * @param shopId
     * @param skuCodes
     * @param warehouseIds
     */
    private void handle(Long shopId, List<String> skuCodes, List<Long> warehouseIds) {
        //店铺商品分组
        List<String> filteredSkuCodes = stockPushLogic.filterShopSkuGroup(shopId, skuCodes);
        log.debug("yunju jit inventory push filteredSkuCodes:{}", filteredSkuCodes == null ? null : filteredSkuCodes.toString());
        //库存推送规则
        Map<String, ShopStockRuleDto> warehouseShopStockRules = stockPushLogic.getWarehouseShopStockRules(shopId,
            filteredSkuCodes);
        log.debug("yunju jit inventory push warehouseShopStockRules:{}",
            warehouseShopStockRules == null ? null : warehouseShopStockRules.toString());
        //过滤仓库商品分组
        Map<String, List<Long>> skuWareshouseIds = stockPushLogic.filterWarehouseSkuGroup(shopId, filteredSkuCodes,
            warehouseIds);
        log.debug("yunju jit inventory push skuWareshouseIds:{}",
            skuWareshouseIds == null ? null : skuWareshouseIds.toString());
        //计算可用可用库存
        Table<String, Long, Long> stocks = yjJitStockPusher.calculate(shopId, skuWareshouseIds,
            warehouseShopStockRules);
        log.debug("yunju jit inventory push stocks:{}", stocks == null ? null : stocks.toString());
        //调用云聚接口推送库存
        yjJitStockPusher.send(shopId, stocks);
    }

}
