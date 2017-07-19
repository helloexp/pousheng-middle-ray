package com.pousheng.middle.warehouse.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 店铺库存分配规则缓存
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18
 */
@Component
@Slf4j
public class ShopStockRuleCacher {

    private final LoadingCache<Long, WarehouseShopStockRule> byShopId;

    public ShopStockRuleCacher(@Value("${cache.duration.in.minutes: 15}")Integer duration,
                               final WarehouseShopStockRuleReadService warehouseShopStockRuleReadService) {
        byShopId = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<Long, WarehouseShopStockRule>() {
                    @Override
                    public WarehouseShopStockRule load(Long shopId) throws Exception {
                        Response<WarehouseShopStockRule> r = warehouseShopStockRuleReadService.findByShopId(shopId);
                        if(!r.isSuccess()){
                            log.error("failed to find WarehouseShopStockRule where shopId={}, error code:{}",
                                    shopId, r.getError());
                            throw new ServiceException(r.getError());
                        }
                        if(r.getResult() == null){
                            log.error("WarehouseShopStockRule(shopId={}) not found", shopId);
                            throw new ServiceException("warehouseShopStockRule.not.found");
                        }
                        return r.getResult();
                    }
                });
    }

    public WarehouseShopStockRule findByShopId(Long shopId){
        return byShopId.getUnchecked(shopId);
    }
}
