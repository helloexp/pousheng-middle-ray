package com.pousheng.middle.consume.index.cacher;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.inventory.api.service.PoushengWarehouseReadService;
import com.pousheng.inventory.domain.dto.PoushengWarehouseDTO;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 17:53<br/>
 */
@Slf4j
@Component
public class WarehouseCacher {
    @RpcConsumer(version = "1.0.0")
    private PoushengWarehouseReadService poushengWarehouseReadService;

    private LoadingCache<Long, Optional<PoushengWarehouseDTO>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadById));

    private Optional<PoushengWarehouseDTO> loadById(Long id) {
        try {
            PoushengWarehouseDTO r = poushengWarehouseReadService.findWarehouseById(id);
            return Optional.ofNullable(r);
        } catch (Exception e) {
            log.error("fail to find warehouse by id: {}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Optional.empty();
        }
    }

    public PoushengWarehouseDTO findById(Long id) {
        return cache.getUnchecked(id).orElse(null);
    }
}
