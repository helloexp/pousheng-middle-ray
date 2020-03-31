/*
 * Copyright (c) 2018. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.item.cacher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description 第三方商品映射关系缓存
 * @Date        2018/12/24
 */
@Component
@Slf4j
public class ItemMappingCacher {

    private LoadingCache<String, List<ItemMapping>> mappingCacher;


    @RpcConsumer
    private MappingReadService mappingReadService;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @PostConstruct
    public void init() {
        this.mappingCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration*5, TimeUnit.SECONDS)
                .maximumSize(5000)
                .build(new CacheLoader<String, List<ItemMapping>>() {
                    @Override
                    public List<ItemMapping> load(String skuCode) throws Exception {
                        Response<List<ItemMapping>> resp = mappingReadService.findBySkuCode(skuCode);
                        if (!resp.isSuccess()) {
                            log.error("failed to find item mappings(skuCode={}), error code:{}",
                                    skuCode, resp.getError());
                            throw new ServiceException("item.mapping.find.fail");
                        }
                        if (resp.getResult().isEmpty()) {
                            log.error("not find item mapping(skuCode={})",
                                    skuCode);
//                            throw new ServiceException("item.mapping.find.fail");

                        }
                        return resp.getResult();
                    }
                });
    }



    public List<ItemMapping> findBySkuCode(String skuCode) {
        return mappingCacher.getUnchecked(skuCode);
    }

    public List<ItemMapping> findBySkuCodeAndShopId(String skuCode,Long shopId) {
        return mappingCacher.getUnchecked(skuCode).stream().filter(itemMapping -> Objects.equals(itemMapping.getOpenShopId(),shopId)).collect(Collectors.toList());
    }

    public void refreshBySkuCode(String skuCode) {
        mappingCacher.refresh(skuCode);
    }
}
