package com.pousheng.middle.schedule.rest;

import com.pousheng.middle.schedule.tasks.SkuFullPushTask;
import com.pousheng.middle.schedule.tasks.SkuStockPartitionerTask;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: 任务接口
 * Author: xiao
 * Date: 2018/05/29
 */
@ConditionalOnProperty(name = "stock.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@RestController
@RequestMapping("/api/middle/schedule")
public class MiddleSchedules {
    private final SkuFullPushTask skuFullPushTask;
    private final SkuStockPartitionerTask skuStockPartitionerTask;

    @Autowired
    public MiddleSchedules(SkuFullPushTask skuFullPushTask, SkuStockPartitionerTask skuStockPartitionerTask) {
        this.skuFullPushTask = skuFullPushTask;
        this.skuStockPartitionerTask = skuStockPartitionerTask;
    }

    @ApiOperation("触发降级任务")
    @GetMapping("/sku-push")
    public String skuFullPush() {
        skuFullPushTask.trigger();
        return "ok";
    }

    @ApiOperation("触发降级任务")
    @GetMapping("/sku-partition")
    public String skuPartition() {
        skuStockPartitionerTask.trigger();
        return "ok";
    }

}
