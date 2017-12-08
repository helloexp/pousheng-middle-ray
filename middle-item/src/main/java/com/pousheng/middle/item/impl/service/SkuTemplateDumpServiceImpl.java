/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.item.impl.service;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import com.pousheng.middle.item.impl.dao.SkuTemplateExtDao;
import com.pousheng.middle.item.service.IndexedSkuTemplateFactory;
import com.pousheng.middle.item.service.IndexedSkuTemplateGuarder;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.impl.dao.SpuAttributeDao;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;
import io.terminus.search.api.IndexExecutor;
import io.terminus.search.api.model.IndexTask;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Author:  songrenfei
 * Date: 2017-11-13
 */
@Slf4j
@Component
@RpcProvider
@SuppressWarnings("unused")
public class SkuTemplateDumpServiceImpl implements SkuTemplateDumpService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SpuDao spuDao;
    @Autowired
    private SkuTemplateDao skuTemplateDao;
    @Autowired
    private SkuTemplateExtDao skuTemplateExtDao;
    @Autowired
    private SpuAttributeDao spuAttributeDao;
    @Autowired
    private IndexExecutor indexExecutor;
    @Autowired
    private IndexedSkuTemplateFactory indexedItemFactory;
    @Autowired
    private SkuTemplateIndexAction indexedItemIndexAction;
    @Autowired
    private SearchSkuTemplateProperties searchSkuTemplateProperties;
    @Autowired
    private IndexedSkuTemplateGuarder indexedSkuTemplateGuarder;




    /**
     * 全量dump商品, 只dump x天有更新的商品
     */
    @Override
    public Response<Boolean> fullDump() {
        try {
            log.info("full dump start");
            Integer fullDumpRange = searchSkuTemplateProperties.getFullDumpRange();
            if (fullDumpRange <= 0) {
                fullDumpRange = 365 * 3;
            }
            String since = DATE_TIME_FORMAT.print(DateTime.now()
                    .withTimeAtStartOfDay()
                    .minusDays(fullDumpRange));
            Stopwatch watch = Stopwatch.createStarted();
            int allIndexed = doIndex(since);
            watch.stop();
            log.info("item full dump end, cost: {} ms, dumped {} items",
                    watch.elapsed(TimeUnit.MILLISECONDS), allIndexed);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to execute items full dump, cause: {}",
                    Throwables.getStackTraceAsString(e));
            return Response.fail("item.dump.fail");
        }
    }


    /**
     * 增量dump商品
     *
     * @param interval 间隔时间(分钟)
     */
    @Override
    public Response<Boolean> deltaDump(Integer interval) {
        try {
            log.info("item delta dump start");
            Stopwatch watch = Stopwatch.createStarted();
            // interval分钟前的时间
            String since = DATE_TIME_FORMAT.print(new DateTime().minusMinutes(interval));
            int allIndexed = doIndex(since);

            watch.stop();
            log.info("item delta dump end, cost: {} ms, dumped {} items",
                    watch.elapsed(TimeUnit.MILLISECONDS), allIndexed);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to execute items delta dump, cause: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.dump.fail");
        }
    }

    private int doIndex(String since) {
        // 最大的商品id
        Long lastId = skuTemplateExtDao.maxId() + 1;
        // 计数本次full dump的商品数
        int allIndexed = 0;
        while (true) {
            List<SkuTemplate> skuTemplates = skuTemplateExtDao.listSince(lastId, since, searchSkuTemplateProperties.getBatchSize());
            if (Iterables.isEmpty(skuTemplates)) {
                break;
            }

            List<Long> spuIds = Lists.transform(skuTemplates, new Function<SkuTemplate, Long>() {
                @Override
                public Long apply(SkuTemplate skuTemplate) {
                    return skuTemplate.getSpuId();
                }
            });

            List<SpuAttribute> spuAttributes = spuAttributeDao.findBySpuIds(spuIds);

            List<Spu> spus = spuDao.findByIds(spuIds);

            Map<Long, Spu> groupSpuById = Maps.uniqueIndex(spus, new Function<Spu, Long>() {
                @Override
                public Long apply(Spu spu) {
                    return spu.getId();
                }
            });


            Map<Long, SpuAttribute> groupSpuAttributebySpuId = Maps.uniqueIndex(spuAttributes, new Function<SpuAttribute, Long>() {
                @Override
                public Long apply(SpuAttribute spuAttribute) {
                    return spuAttribute.getSpuId();
                }
            });

            for (SkuTemplate skuTemplate : skuTemplates) {
                try {
                    if (indexedSkuTemplateGuarder.indexable(skuTemplate)) {  //更新
                        IndexedSkuTemplate indexedItem = indexedItemFactory.create(skuTemplate, groupSpuById.get(skuTemplate.getSpuId()),groupSpuAttributebySpuId.get(skuTemplate.getSpuId()));
                        IndexTask indexTask = indexedItemIndexAction.indexTask(indexedItem);
                        indexExecutor.submit(indexTask);
                    } else { //删除
                        IndexTask indexTask = indexedItemIndexAction.deleteTask(skuTemplate.getId());
                        indexExecutor.submit(indexTask);
                    }
                } catch (Exception e) {
                    log.error("failed to index skuTemplate(id={}),cause:{}", skuTemplate.getId(), Throwables.getStackTraceAsString(e));
                }
            }

            allIndexed += skuTemplates.size();
            log.info("has indexed {} items,and last handled id is {}", allIndexed, lastId);
            lastId = Iterables.getLast(skuTemplates).getId();
            // 已经是最后一批商品数据
            if (skuTemplates.size() < searchSkuTemplateProperties.getBatchSize()) {
                break;
            }
        }
        return allIndexed;
    }

}
