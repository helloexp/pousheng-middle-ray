package com.pousheng.middle.web.shop.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.web.shop.component.OpenShopLogic;
import com.pousheng.middle.web.shop.dto.ShopChannelGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by songrenfei on 2018/4/1
 */
@Slf4j
@Component
public class ShopChannelGroupCacher {

    private LoadingCache<String, List<ShopChannelGroup>> allShopChannelGroupCache;

    private static final String KEY = "all-shop-channel-group";

    @Autowired
    private OpenShopLogic openShopLogic;

    @Value("${cache.duration.in.minutes: 30}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.allShopChannelGroupCache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, List<ShopChannelGroup>>() {
                    @Override
                    public List<ShopChannelGroup> load(String key) throws Exception {
                        return openShopLogic.findShopChannelGroup();
                    }
                });
    }

    public List<ShopChannelGroup> listAllShopChannelGroupCache() {
        return allShopChannelGroupCache.getUnchecked(KEY);
    }

    /**
     * 刷新缓存
     */
    public void refreshShopChannelGroupCache() {
        allShopChannelGroupCache.refresh(KEY);
    }
}
