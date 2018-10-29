package com.pousheng.middle.web.item;

import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhaoxw
 * @date 2018/5/3
 */

@Slf4j
@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "false", matchIfMissing = false)
@RestController
public class AutoDeleteStcokLogs {

    @RpcConsumer
    private SkuTemplateDumpService skuTemplateDumpService;

    @RpcConsumer
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;

    @Autowired
    private HostLeader hostLeader;

    private static String STOCK_LOG_INDEX = "stocklogs";

    private static String STOCK_LOG_TYPE = "stocklog";

    private static Integer DAYS = 30;


    /**
     * 每天凌晨1点触发
     */
    @RequestMapping("/api/del/stock/log")
    @Scheduled(cron = "0 0 1 * * ?")
    public void synchronizeSpu() {
        if (hostLeader.isLeader()) {
            log.info("START JOB DELETE STOCK LOG");
            skuTemplateDumpService.batchDelete(STOCK_LOG_INDEX, STOCK_LOG_TYPE, DAYS);
            DateTime today = new DateTime().dayOfWeek().roundFloorCopy();
            middleStockPushLogWriteService.deleteByBeforeDate(today.minusDays(DAYS).toDate());
            log.info("END JOB DELETE STOCK LOG");
        } else {
            log.info("host is not leader, so skip job");
        }
    }

}
