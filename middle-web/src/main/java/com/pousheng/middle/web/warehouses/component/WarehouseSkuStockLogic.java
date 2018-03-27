package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2018/3/27
 */
@Component
@Slf4j
public class WarehouseSkuStockLogic {

    @Autowired
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;


    public Response<Map<String,Integer>> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes){

        Response<List<WarehouseSkuStock>> listRes = warehouseSkuReadService.findSkuStocks(warehouseId,skuCodes);
        List<WarehouseSkuStock> stocks = listRes.getResult();
        Map<String, Integer> r = Maps.newHashMapWithExpectedSize(skuCodes.size());
        for (WarehouseSkuStock stock : stocks) {

            Warehouse warehouse = warehouseCacher.findById(stock.getWarehouseId());
            Map<String, String> extra = warehouse.getExtra();
            if(!extra.containsKey("outCode")){
                log.error("warehouse(id:{}) out code is null,so skip to count stock");
                continue;
            }
            String outerId = extra.get("outCode");
            String companyId = warehouse.getCompanyId();
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(outerId,Long.valueOf(companyId));
            //如果是店仓则要减掉中台的占用库存
            if(Objects.equal(warehouse.getType(),1)){
                Long localStock =  dispatchComponent.getMposSkuShopLockStock(shop.getId(),stock.getSkuCode());
                Long availStock = stock.getAvailStock() - localStock;
                if(availStock<=0L){
                    availStock =0L;
                }
                r.put(stock.getSkuCode(), availStock.intValue());
            }else {
                r.put(stock.getSkuCode(), stock.getAvailStock().intValue());
            }
        }
        return Response.ok(r);
    }
}
