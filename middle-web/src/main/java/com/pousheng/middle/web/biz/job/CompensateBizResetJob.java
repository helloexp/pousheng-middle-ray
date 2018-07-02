package com.pousheng.middle.web.biz.job;

import com.pousheng.middle.order.service.AutoCompensationWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by penghui on 2018/6/12
 */
@RestController
@Slf4j
public class CompensateBizResetJob {

    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private AutoCompensationWriteService autoCompensationWriteService;

    @RequestMapping(value = "/async/job/reset", method = RequestMethod.GET)
    @Scheduled(cron = "0 */5 * * * ?")
    public void reset() {
        log.info("start to reset long time handling task....");
        if (!hostLeader.isLeader()) {
            log.info("current leader is {}, skip", hostLeader.currentLeaderId());
            return;
        }
        Response<Boolean> response = autoCompensationWriteService.resetStatus();
        if (!response.isSuccess()) {
            log.error("reset long time handling task failed,cause:{}", response.getError());
        }
        Response<Boolean> response1 = poushengCompensateBizWriteService.resetStatus();
        if (!response1.isSuccess()) {
            log.error("reset long time handling task failed,cause:{}", response1.getError());
        }
        log.info("end to reset long time handling task....");
    }
}
