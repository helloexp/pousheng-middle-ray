package com.pousheng.middle.web.item.cacher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxw
 * @date 2018/5/14
 */
@Component
@Slf4j
public class ItemGroupCacher {

    private LoadingCache<Long, ItemGroup> groupCacher;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @RpcConsumer
    private ItemGroupReadService itemGroupReadService;

    @PostConstruct
    public void init() {
        this.groupCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<Long, ItemGroup>() {
                    @Override
                    public ItemGroup load(Long id) {
                        Response<ItemGroup> resp = itemGroupReadService.findById(id);
                        if (!resp.isSuccess()) {
                            log.error("failed to find rule (shopId={}, error:{}",
                                    id, resp.getError());
                            throw new ServiceException("item.group.find.fail");
                        }
                        return resp.getResult();
                    }
                });
    }

    public ItemGroup findById(Long id) {
        return groupCacher.getUnchecked(id);
    }

    /**
     * 刷新全部缓存，用于删除rule之后的缓存清理
     */
    public void refreshAll() {
        groupCacher.invalidateAll();
    }
}

