package com.pousheng.middle.web.warehouses.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @Value("${skx.warehouse.id}")
    private Long skxWarehouseId;


    /**
     * 根据仓库id和商品条码查询对应的库存
     */
    public Response<Map<String,Integer>> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes){

        Warehouse warehouse = warehouseCacher.findById(warehouseId);
        Map<String, String> extra = warehouse.getExtra();
        if( !extra.containsKey("outCode")){
            log.error("warehouse(id:{}) out code is null,so skip to count stock");
            return Response.fail("warehouse.out.code.invalid");
        }

        Response<List<WarehouseSkuStock>> listRes = warehouseSkuReadService.findSkuStocks(warehouseId,skuCodes);
        List<WarehouseSkuStock> stocks = listRes.getResult();

        Map<String, WarehouseSkuStock> skuCodeMap = stocks.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(WarehouseSkuStock::getSkuCode, it -> it));

        Map<String, Integer> r = Maps.newHashMapWithExpectedSize(skuCodes.size());

        for (String skuCode : skuCodes) {

            WarehouseSkuStock stock = null;
            if( skuCodeMap.containsKey(skuCode)){
                stock = skuCodeMap.get(skuCode);
            }

            //查询恒康库存
            String outerId = extra.get("outCode");
            String companyId = warehouse.getCompanyId();
            List<HkSkuStockInfo> hkSkuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(Lists.newArrayList(warehouseId),Lists.newArrayList(skuCode));

            if( Objects.equals(warehouseId,skxWarehouseId)){

                if (Arguments.isNull(stock)){
                    log.error("not find stock by warehouse id:{} sku code:{}",warehouseId,skuCode);
                    continue;
                }
                r.put(skuCode, stock.getAvailStock().intValue());

            //如果是店仓则要减掉中台的占用库存
            } else if( Objects.equals(warehouse.getType(),1)){
                Shop shop = middleShopCacher.findByOuterIdAndBusinessId(outerId,Long.valueOf(companyId));
                ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                if( Arguments.isNull(shopExtraInfo)){
                    log.error("not find shop(id:{}) extra info by shop extra info json:{} ",shop.getId(),shop.getExtra());
                    throw new ServiceException("shop.safe.stock.invalid");
                }

                if( CollectionUtils.isEmpty(hkSkuStockInfos)){
                    log.error("not query stock from hk where stock code:{} sku code:{}",outerId,skuCode);
                    continue;
                }
                //可用库存
                Integer hkAvailStock = hkSkuStockInfos.get(0).getMaterial_list().get(0).getQuantity();
                //安全库存
                Integer safeStock = Arguments.isNull(shopExtraInfo.getSafeStock())? 0 : shopExtraInfo.getSafeStock();
                Long localStock =  dispatchComponent.getMposSkuShopLockStock(shop.getId(),skuCode);
                Long availStock = hkAvailStock - localStock - safeStock;
                if( availStock <= 0L){
                    availStock = 0L;
                }
                r.put(skuCode, availStock.intValue());
            } else {
                //判断是否为mpos仓，则是用实时恒康库存，并减掉中台安全库存
                Integer safeStock = 0;
                if( Objects.equals(warehouse.getIsMpos(),1)){
                    if( CollectionUtils.isEmpty(extra) ||! extra.containsKey("safeStock")){
                        log.error("not find safe stock for warehouse:(id:{})",warehouse.getId());
                    } else {
                        //安全库存
                        safeStock = Integer.valueOf(extra.get("safeStock"));
                    }
                    if( CollectionUtils.isEmpty(hkSkuStockInfos)){
                        log.error("not query stock from hk where stock code:{} sku code:{}",outerId,skuCode);
                        continue;
                    }
                    //可用库存
                    Integer hkAvailStock = hkSkuStockInfos.get(0).getMaterial_list().get(0).getQuantity();
                    r.put(skuCode, hkAvailStock - safeStock);
                } else {
                    //非mpos大仓，则直接取中台库存
                    if (Arguments.isNull(stock)){
                        log.error("not find stock by warehouse id:{} sku code:{}",warehouseId,skuCode);
                        continue;
                    }
                    r.put(skuCode, stock.getAvailStock().intValue());
                }
            }
        }
        return Response.ok(r);
    }
}
