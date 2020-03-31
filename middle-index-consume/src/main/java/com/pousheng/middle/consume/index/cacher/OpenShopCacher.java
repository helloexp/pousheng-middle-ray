package com.pousheng.middle.consume.index.cacher;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-14 16:01<br/>
 */
@Slf4j
@Component
public class OpenShopCacher {
    @RpcConsumer
    private OpenShopReadService openShopReadService;

    private LoadingCache<Long, Optional<OpenShop>> shopCacher = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadById));

    private Optional<OpenShop> loadById(Long shopId) {
        try {
            Response<OpenShop> r = openShopReadService.findById(shopId);
            if (!r.isSuccess()) {
                log.error("failed to , cause{}", r.getError());
                return Optional.empty();
            }
            return Optional.ofNullable(r.getResult());
        } catch (Exception e) {
            log.error("failed to find open shop by id: {}, cause: {}", shopId, Throwables.getStackTraceAsString(e));
            return Optional.empty();
        }
    }

    public OpenShop findById(Long shopId) {
        return shopCacher.getUnchecked(shopId).orElse(null);
    }
}
