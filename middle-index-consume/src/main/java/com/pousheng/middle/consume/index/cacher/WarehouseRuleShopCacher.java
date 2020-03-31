package com.pousheng.middle.consume.index.cacher;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.inventory.api.service.WarehouseShopGroupReadService;
import com.pousheng.inventory.domain.model.WarehouseShopGroup;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 17:53<br/>
 */
@Slf4j
@Component
public class WarehouseRuleShopCacher {
    @RpcConsumer(version = "1.0.0")
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    private LoadingCache<Long, List<WarehouseShopGroup>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadById));

    private List<WarehouseShopGroup> loadById(Long groupId) {
        try {
            Response<List<WarehouseShopGroup>> r = warehouseShopGroupReadService.findByGroupId(groupId);
            if (!r.isSuccess()) {
                throw new ServiceException(r.getError());
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("fail to find warehouse shop group by id: {}, cause:{}", groupId, Throwables.getStackTraceAsString(e));
            throw e;
        }
    }

    public List<WarehouseShopGroup> findById(Long groupId) {
        return cache.getUnchecked(groupId);
    }
}
