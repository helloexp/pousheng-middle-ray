package com.pousheng.middle.open;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.item.service.ItemServiceCenter;
import io.terminus.open.client.item.service.PushedItemReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-14
 */
@Component
@Slf4j
public class StockPusher {

    private final ExecutorService executorService;

    @RpcConsumer
    private PushedItemReadService pushedItemReadService;

    @RpcConsumer
    private ItemServiceCenter itemServiceCenter;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    private LoadingCache<String, Long> skuCodeCacher;

    @Autowired
    public StockPusher(@Value("${index.queue.size: 10000}") int queueSize,
                       @Value("${cache.duration.in.minutes: 60}") int duration) {
        this.executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.MINUTES, new ArrayBlockingQueue(queueSize), (new ThreadFactoryBuilder()).setNameFormat("search-indexer-%d").build(), new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.error("task {} is rejected", r);
            }
        });

        this.skuCodeCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration*24,TimeUnit.MINUTES)
                .maximumSize(200000)
                .build(new CacheLoader<String, Long>() {
                    @Override
                    public Long load(String skuCode) throws Exception {
                        Response<List<SkuTemplate>> r = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
                        if(!r.isSuccess()){
                            log.error("failed to find skuTemplate(skuCode={}),error code:{}", skuCode, r.getError());
                            throw new ServiceException("skuTemplate.find.fail");
                        }
                        List<SkuTemplate> skuTemplates = r.getResult();
                        if(CollectionUtils.isEmpty(skuTemplates)){
                            log.error("skuTemplate(skuCode={}) not found", skuCode);
                            throw new ServiceException("skuTemplate.not.found");
                        }
                        return skuTemplates.get(0).getSpuId();
                    }
                });
    }

    public void submit(final String skuCode) {
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {
                Long spuId = skuCodeCacher.getUnchecked(skuCode);
                //找到对应的店铺id, 这些店铺需要进行库存推送
                Response<List<Long>> r =pushedItemReadService.findPushedOpenShopIdsOfItem(spuId);
                if(!r.isSuccess()){
                    log.error("failed to find out shops for spu(id={}), error code:{}", spuId, r.getError());
                    return;
                }

                //计算库存分配并将库存推送到每个外部店铺去
                List<Long> shopIds = r.getResult();
                for (Long shopId : shopIds) {
                    try{
                        //todo: 计算每个店铺的可用库存
                        Integer stock =0;
                        Response<Boolean> rP = itemServiceCenter.updateSkuStock(shopId, skuCode, stock);
                        if(!rP.isSuccess()){
                            log.error("failed to push stock of sku(skuCode={}) to shop(id={}), error code{}",
                                    skuCode, shopId, rP.getError());
                        }
                    }catch (Exception e){
                        log.error("failed to push stock of sku(skuCode={}) to shop(id={}), cause: {}",
                                skuCode, shopId, Throwables.getStackTraceAsString(e));
                    }
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        this.executorService.shutdown();
    }

}
