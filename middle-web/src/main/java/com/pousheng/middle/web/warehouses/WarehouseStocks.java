package com.pousheng.middle.web.warehouses;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.warehouses.dto.SkuStock;
import com.pousheng.middle.web.warehouses.dto.SkuStockDetail;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 库存管理部分
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@RestController
@RequestMapping("/api/warehouse")
@Slf4j
public class WarehouseStocks {

    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;

    @RpcConsumer
    private WarehouseSkuWriteService skuWriteService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    /**
     * sku库存概览, 不分仓
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回条数
     * @param skuCode  sku码查询
     * @return 查询结果
     */
    @RequestMapping(value = "/stock/summary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuStock> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                   @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                   @RequestParam(required = false, value = "skuCode") String skuCode,
                                   @RequestParam(required = false, value = "spuId") Long spuId) {

        Map<String, Object> params = Maps.newHashMap();
        if (StringUtils.hasText(skuCode)) {
            params.put("skuCode", skuCode);
        }
        if (spuId != null) {
            Response<List<SkuTemplate>> rST = skuTemplateReadService.findBySpuId(spuId);
            if (!rST.isSuccess()) {
                log.error("failed to find skuTemplates by spuId={}, error code:{}", spuId, rST.getError());
            }
            List<String> skuCodes = Lists.newArrayListWithCapacity(rST.getResult().size());
            for (SkuTemplate skuTemplate : rST.getResult()) {
                skuCodes.add(skuTemplate.getSkuCode());
            }
            if (!CollectionUtils.isEmpty(skuCodes)) {
                params.put("skuCodes", skuCodes);
            }else{
                return Paging.empty();
            }
        }
        Response<Paging<WarehouseSkuStock>> r = warehouseSkuReadService.findBy(pageNo, pageSize, params);
        if (!r.isSuccess()) {
            log.error("failed to find warehouse sku stock by params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        Paging<WarehouseSkuStock> p = r.getResult();
        List<WarehouseSkuStock> warehouseSkuStocks = p.getData();
        if (!CollectionUtils.isEmpty(warehouseSkuStocks)) {
            List<String> skuCodes = Lists.newArrayListWithCapacity(warehouseSkuStocks.size());
            for (WarehouseSkuStock warehouseSkuStock : warehouseSkuStocks) {
                skuCodes.add(warehouseSkuStock.getSkuCode());
            }
            Response<List<SkuTemplate>> rST = skuTemplateReadService.findBySkuCodes(skuCodes);
            if (!rST.isSuccess()) {
                log.error("failed to find skuTemplates by skuCodes:{}, error code:{}", skuCodes, rST.getError());
                throw new JsonResponseException(rST.getError());
            }
            Map<String, SkuTemplate> bySkuCode = Maps.uniqueIndex(rST.getResult(), new Function<SkuTemplate, String>() {
                @Nullable
                @Override
                public String apply(@Nullable SkuTemplate input) {
                    return input.getSkuCode();
                }
            });
            List<SkuStock> result = Lists.newArrayListWithCapacity(warehouseSkuStocks.size());
            for (WarehouseSkuStock warehouseSkuStock : warehouseSkuStocks) {
                SkuStock skuStock = new SkuStock();
                String currentSkuCode = warehouseSkuStock.getSkuCode();
                SkuTemplate skuTemplate = bySkuCode.get(currentSkuCode);
                if (skuTemplate == null) {
                    log.error("no skuTemplate found for skuCode:{}, ignore", currentSkuCode);
                    continue;
                }
                skuStock.setName(skuTemplate.getName());
                skuStock.setSkuCode(currentSkuCode);
                skuStock.setSkuId(skuTemplate.getId());
                skuStock.setStock(warehouseSkuStock.getAvailStock());
                skuStock.setSkuAttrs(skuTemplate.getAttrs());
                skuStock.setSpuId(skuTemplate.getSpuId());
                result.add(skuStock);
            }
            return new Paging<>(p.getTotal(), result);
        }

        return Paging.empty();
    }


    /**
     * 在一个仓库中对应sku的库存
     *
     * @param skuCodes sku codes, 以','分割
     * @return sku在对应仓库中的可用库存情况
     */
    @RequestMapping(value = "/{warehouseId}/stocks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Integer> findStocksForSkus(@PathVariable("warehouseId") Long warehouseId,
                                                  @RequestParam("skuCodes") String skuCodes) {

        List<String> skuCodeList = Lists.newArrayList(Splitters.COMMA.splitToList(skuCodes));
        if (CollectionUtils.isEmpty(skuCodeList)) {
            return Collections.emptyMap();
        }
        Response<Map<String, Integer>> r = warehouseSkuReadService.findByWarehouseIdAndSkuCodes(warehouseId, skuCodeList);
        if (!r.isSuccess()) {
            log.error("failed to find stock in warehouse(id={}) for skuCodes:{}, error code:{}",
                    warehouseId, skuCodes, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    @RequestMapping(value = "/stock-detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SkuStockDetail stockDetails(@RequestParam("skuCode") String skuCode) {
        Response<List<WarehouseSkuStock>> r = warehouseSkuReadService.findBySkuCode(skuCode);
        if (!r.isSuccess()) {
            log.error("failed to find stock detail for sku(skuCode={}), error code:{}", skuCode, r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<WarehouseSkuStock> stocks = r.getResult();

        SkuStockDetail detail = new SkuStockDetail();
        Long total = 0L;
        List<SkuStockDetail.StockInWarehouse> details = Lists.newArrayListWithCapacity(stocks.size());
        for (WarehouseSkuStock stock : stocks) {
            total = total + stock.getAvailStock();
            SkuStockDetail.StockInWarehouse siw = new SkuStockDetail.StockInWarehouse();
            siw.setId(stock.getId());
            siw.setQuantity(stock.getAvailStock());
            Warehouse warehouse = warehouseCacher.findById(stock.getWarehouseId());
            siw.setWarehouseInnerCode(warehouse.getExtra().get("outCode"));
            siw.setWarehouseName(warehouse.getName());
            details.add(siw);
        }
        detail.setTotal(total);
        detail.setDetails(details);
        return detail;
    }


}
