/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.job;

import com.pousheng.middle.item.service.SkuTemplateDumpService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: songrenfei
 * Date: 2017-11-11
 */
@RestController
@RequestMapping("/api/handle/sku/template/")
@Slf4j
public class HandleSkuTemplateDumps {

    @RpcConsumer
    private SkuTemplateDumpService skuTemplateDumpService;

    @RequestMapping(value = "full", produces = MediaType.APPLICATION_JSON_VALUE)
    public void fullDump(){
        log.info("sku template full dump fired");
        skuTemplateDumpService.fullDump();
        log.info("sku template full dump end");
    }

    @RequestMapping(value = "delta", produces = MediaType.APPLICATION_JSON_VALUE)
    public void deltaDump(Integer time){
        log.info("START JOB SkuTemplateDumps.deltaDump");
        skuTemplateDumpService.deltaDump(time);
        log.info("START JOB SkuTemplateDumps.deltaDump");
    }
}
