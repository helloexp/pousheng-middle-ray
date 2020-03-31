package com.pousheng.middle.web.yintai.job;

import com.pousheng.middle.web.yintai.component.MiddleYintaiItemService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 银泰商品推送任务
 * AUTHOR: zhangbin
 * ON: 2019/6/27
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "yintai.item.push.job.enable", havingValue = "true", matchIfMissing = false)
public class YintaiUploadItemJob {

    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private MiddleYintaiItemService middleYintaiItemService;

    @Value("${yintai.item.full.push.hour:72}")
    private Integer fullHour;
    @Value("${yintai.item.increment.push.hour:3}")
    private Integer incrementHour;
    /**
     * 每天全量执行上传，3天内
     */
    @Scheduled(cron = "${yintai.item.push.day.cron:0 0 * * * ?}")
    public void everydayJob() {

        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("yintai everydayJob start ");
        middleYintaiItemService.uploadTask(fullHour);
        log.info("yintai everydayJob end ");
    }

    /**
     * 每小时增量执行上传，3小时内
     */
    @Scheduled(cron = "${yintai.item.push.hour.cron:0 0 1-23 * * ?}")
    public void everyHourJob() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("yintai everyHourJob start ");
        middleYintaiItemService.uploadTask(incrementHour);
        log.info("yintai everyHourJob end ");
    }

}
