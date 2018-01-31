/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.job;

import com.pousheng.middle.item.service.SkuTemplateDumpService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.parana.search.item.ItemDumpService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: songrenfei
 * Date: 2017-11-11
 */
@RestController
@RequestMapping("/api/search/sku/template/")
@Slf4j
public class SkuTemplateDumps {

    @RpcConsumer
    private SkuTemplateDumpService skuTemplateDumpService;

    @Autowired
    private HostLeader hostLeader;

    @RequestMapping(value = "full", produces = MediaType.APPLICATION_JSON_VALUE)
    //@Scheduled(cron="0 0 1 * * ?")
    public void fullDump(){  //每天凌晨一点执行一次
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("sku template full dump fired");
        skuTemplateDumpService.fullDump();
        log.info("sku template full dump end");
    }

    @Scheduled(cron = "0 */15 * * * ?")
    @RequestMapping(value = "delta", produces = MediaType.APPLICATION_JSON_VALUE)
    public void deltaDump(){ //每隔15分钟执行一次
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip ", hostLeader.currentLeaderId());
            return;
        }
        log.info("sku template delta dump fired");
        skuTemplateDumpService.deltaDump(15);
        log.info("sku template delta dump end");
    }
}