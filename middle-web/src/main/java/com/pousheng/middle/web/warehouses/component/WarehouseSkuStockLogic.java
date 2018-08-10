package com.pousheng.middle.web.warehouses.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2018/3/27
 */
@Component
@Slf4j
public class WarehouseSkuStockLogic {

    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private WarehouseCacher warehouseCacher;

    @Value("${skx.warehouse.id}")
    private Long skxWarehouseId;


    /**
     * 根据仓库id和商品条码查询对应的库存
     */
    public Response<Map<String,Integer>> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes, Long shopId){

        WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
        if(StringUtils.isBlank(warehouse.getOutCode())){
            log.error("warehouse(id:{}) out code is null,so skip to count stock");
            return Response.fail("warehouse.out.code.invalid");
        }

        Map<String, Integer> r = Maps.newHashMapWithExpectedSize(skuCodes.size());

        if(Objects.equals(warehouseId,skxWarehouseId)) {
            Response<List<InventoryDTO>> listRes = inventoryClient.findSkuStocks(warehouseId,skuCodes);
            if (!listRes.isSuccess()) {
                log.error("call inventory to find inventory list fail, warehouseId:{} sku code:{}, caused: {}",
                        warehouseId,skuCodes, listRes.getError());

                return Response.ok(r);
            }
            List<InventoryDTO> stocks = listRes.getResult();

            Map<String, InventoryDTO> skuCodeMap = stocks.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(InventoryDTO::getSkuCode, it -> it));

            for (String skuCode : skuCodes) {
                InventoryDTO stock = null;
                if( skuCodeMap.containsKey(skuCode)){
                    stock = skuCodeMap.get(skuCode);
                }

                if (Arguments.isNull(stock)){
                    log.error("not find stock by warehouse id:{} sku code:{}",warehouseId,skuCode);
                    continue;
                }
                r.put(skuCode, stock.getAvailStockExcludeSafe().intValue());
            }

            return Response.ok(r);
        }

        // 直接查询库存中心可用库存接口
        Response<List<AvailableInventoryDTO>> availableInvRes = inventoryClient.getAvailableInventory(
                Lists.transform(skuCodes, input -> AvailableInventoryRequest.builder().skuCode(input).warehouseId(warehouseId).build()), shopId);
        if (!availableInvRes.isSuccess() || ObjectUtils.isEmpty(availableInvRes.getResult())) {
            log.error("not query inventory where warehouseId:{} sku code:{}",warehouseId,skuCodes);

            return Response.ok(r);
        }

        for (AvailableInventoryDTO availableInventoryDTO : availableInvRes.getResult()) {
            r.put(availableInventoryDTO.getSkuCode(), availableInventoryDTO.getAvailableQuantityWithoutSafe());
        }

        return Response.ok(r);
    }
}
