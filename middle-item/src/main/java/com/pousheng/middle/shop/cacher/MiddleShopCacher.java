/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.cacher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-04-26
 */
@Component
@Slf4j
public class MiddleShopCacher {

    private LoadingCache<String, Shop> shopCacher;

    @RpcConsumer
    private ShopReadService shopReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.shopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<String, Shop>() {
                    @Override
                    public Shop load(String outerId) throws Exception {
                        Response<Shop> rShop = shopReadService.findByOuterId(outerId);
                        if (!rShop.isSuccess()) {
                            log.error("failed to find shop(outerId={}), error code:{}",
                                    outerId, rShop.getError());
                            throw new ServiceException("find shop fail,error code: " + rShop.getError());
                        }
                        return rShop.getResult();
                    }
                });
    }

    /**
     * 根据outerId查找shop的信息
     *
     * @param outerId shop outer id
     * @return 对应shop信息
     */
    public Shop findShopByOuterId(String outerId) {
        return shopCacher.getUnchecked(outerId);
    }
}
