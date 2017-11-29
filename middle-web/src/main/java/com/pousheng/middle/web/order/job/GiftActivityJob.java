package com.pousheng.middle.web.order.job;

import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityEvent;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityStatus;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.web.order.component.PoushengGiftActivityReadLogic;
import com.pousheng.middle.web.order.component.PoushengGiftActivityWriteLogic;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@Component
@Slf4j
public class GiftActivityJob {
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private PoushengGiftActivityWriteLogic poushengGiftActivityWriteLogic;
    @Autowired
    private PoushengGiftActivityReadLogic poushengGiftActivityReadLogic;

    /**
     * 该调度任务的主要作用是状态未开始但是到了开始时间的活动状态修改为进行中，
     * 将进行中的活动一旦到了结束时间，则状态修改为已结束
     */
    @Scheduled(cron = "0 0/1 * * * ? ")
    public void doGiftActvity() {
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START SCHEDULE ON POUSNENG GIFT ACTIVITY");
        List<PoushengGiftActivity> waitStartActivities =  poushengGiftActivityReadLogic.findByStatus(PoushengGiftActivityStatus.WAIT_START.getValue(),PoushengGiftActivityStatus.WAIT_DONE.getValue());
        for (PoushengGiftActivity activity:waitStartActivities){
            long current = System.currentTimeMillis();
            long activityStartTime = activity.getActivityStartAt().getTime();
            if (current>=activityStartTime){
                try{
                    poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(activity.getId(), PoushengGiftActivityEvent.HANDLE.toOrderOperation());
                }catch (Exception e){
                    log.error("update PoushengGiftActivity failed,id is {},caused by {}",activity.getId(),e.getMessage());
                }
            }
        }
        log.info("END SCHEDULE ON POUSNENG GIFT ACTIVITY");
    }
}
