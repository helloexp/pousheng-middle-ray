/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.shop.cacher;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-04-26
 */
@Component
@Slf4j
public class MiddleShopCacher {

    private LoadingCache<String, Shop> shopCacher;

    private LoadingCache<Long, OpenShop> openShopCacher;

    @RpcConsumer
    private PsShopReadService shopReadService;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.shopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration*1, TimeUnit.MINUTES)
                .maximumSize(4000)
                .build(new CacheLoader<String, Shop>() {
                    @Override
                    public Shop load(String joinStr) throws Exception {
                        log.info("joinStr:{}",joinStr);
                        List<String> stringList = Splitters.COLON.splitToList(joinStr);
                        String outerId = stringList.get(0);
                        Long businessId = Long.valueOf(stringList.get(1));
                        Response<Optional<Shop>> rShop = shopReadService.findByOuterIdAndBusinessId(outerId, businessId);
                        if (!rShop.isSuccess()) {
                            log.error("failed to find shop(outerId={},businessId={}), error code:{}",
                                    outerId, businessId, rShop.getError());
                            throw new ServiceException("find.shop.fail");
                        }
                        if (!rShop.getResult().isPresent()) {
                            log.error("not find shop(outerId={},businessId={})",
                                    outerId, businessId);
                            throw new ServiceException("shop.not.exist");

                        }
                        return rShop.getResult().get();
                    }
                });
        this.openShopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration*1, TimeUnit.MINUTES)
                .maximumSize(4000)
                .build(new CacheLoader<Long, OpenShop>() {
                    @Override
                    public OpenShop load(Long id) {
                        Response<OpenShop> rShop = openShopReadService.findById(id);
                        if (!rShop.isSuccess()) {
                            log.error("failed to find open shop(id={}), error code:{}",
                                    id, rShop.getError());
                            throw new ServiceException("find.shop.fail");
                        }
                        if (rShop.getResult() == null) {
                            log.error("not  find open shop(id={})", id);
                            throw new ServiceException("shop.not.exist");

                        }
                        return rShop.getResult();
                    }
                });
    }


    /**
     * 根据outerId查找shop的信息
     *
     * @param outerId    shop outer id
     * @param businessId shop business id
     * @return 对应shop信息
     */
    public Shop findByOuterIdAndBusinessId(String outerId, Long businessId) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("findByOuterIdAndBusinessId by outerId:{} and businessId:{}",outerId,businessId);
        log.info("current shopCacher size:{}",this.shopCacher.asMap().size());
        Shop shop = shopCacher.getUnchecked(Joiners.COLON.join(outerId, businessId));
        stopwatch.stop();
        log.info("end to findByOuterIdAndBusinessId,and cost {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));
        return shop;
    }

    /**
     * 根据ID查询店铺信息
     *
     * @param id open Shop id
     * @return 对应shop信息
     */
    public OpenShop findById(Long id) {
        return openShopCacher.getUnchecked(id);
    }

    /**
     * 根据outerId刷新shop的信息
     *
     * @param outerId    shop outer id
     * @param businessId shop business id
     * @return 对应shop信息
     */
    public void refreshByOuterIdAndBusinessId(String outerId, Long businessId) {
        shopCacher.refresh(Joiners.COLON.join(outerId, businessId));
    }
}
