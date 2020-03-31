package com.pousheng.middle.consume.index.cacher;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Longs;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-17 14:47<br/>
 */
@Slf4j
@Component
public class ShopCacher {
    @RpcConsumer
    private PsShopReadService psShopReadService;

    private Splitter DASH = Splitter.on("-").omitEmptyStrings().trimResults();
    private LoadingCache<ShopKey, Optional<Shop>> keyToShop = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadByKey));

    private Optional<Shop> loadByKey(ShopKey key) {
        try {
            Response<Optional<Shop>> r = psShopReadService.findByOuterIdAndBusinessId(key.getOuterId(), key.getBizId());
            if (!r.isSuccess()) {
                log.error("failed to find shop by key: {}, cause: {}", key, r.getError());
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("fail to find shop by key: {}, cause:{}", key, Throwables.getStackTraceAsString(e));
            return Optional.absent();
        }
    }

    /**
     * 根据 open shop key 查找，类似 213-SP000096
     */
    public Shop findByAppKey(String key) {
        if (StringUtils.isEmpty(key) || !key.contains("-")) {
            return null;
        }

        List<String> keys = DASH.splitToList(key);
        if (keys.size() <= 1) {
            return null;
        }

        Long bizId = Longs.tryParse(keys.get(0));
        return keyToShop
                .getUnchecked(new ShopKey(bizId, keys.get(1)))
                .orNull();
    }
    /**
     * 根据公司代码，与外码查找
     */
    public Shop findByCompanyAndCode(String companyId, String code) {
        Long bizId = Longs.tryParse(companyId);
        return keyToShop
                .getUnchecked(new ShopKey(bizId, code))
                .orNull();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ShopKey implements Serializable {
        private static final long serialVersionUID = -5667473351412747636L;

        private Long bizId;
        private String outerId;
    }
}
