package com.pousheng.middle.web.order.job;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityEvent;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityStatus;
import com.pousheng.middle.order.enums.PoushengGiftQuantityRule;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.web.order.component.PoushengGiftActivityReadLogic;
import com.pousheng.middle.web.order.component.PoushengGiftActivityWriteLogic;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
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
    @Scheduled(cron = "0 0/3 * * * ? ")
    public void doGiftActvity() {
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START JOB GiftActivityJob.doGiftActvity");
        List<PoushengGiftActivity> waitStartActivities =  poushengGiftActivityReadLogic.findByStatus(PoushengGiftActivityStatus.WAIT_START.getValue());
        for (PoushengGiftActivity activity:waitStartActivities){
            long current = System.currentTimeMillis();//当前时间
            long activityStartTime = activity.getActivityStartAt().getTime();//活动开始时间
            if (current>=activityStartTime){
                try{
                    poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(activity.getId(), PoushengGiftActivityEvent.HANDLE.toOrderOperation());
                }catch (Exception e){
                    log.error("update PoushengGiftActivity failed,id is {},caused by {}",activity.getId(), Throwables.getStackTraceAsString(e));
                }
            }
        }

        List<PoushengGiftActivity> doingActivities =  poushengGiftActivityReadLogic.findByStatus(PoushengGiftActivityStatus.WAIT_DONE.getValue());
        for (PoushengGiftActivity activity:doingActivities){
            long current = System.currentTimeMillis();//当前时间
            long activityEndTime = activity.getActivityEndAt().getTime();//活动结束时间
            //判断是否限制参与人数
            boolean isEnoughQuantity = false;
            if (Objects.equals(activity.getQuantityRule(), PoushengGiftQuantityRule.LIMIT_PARTICIPANTS)){
                long alreadyActivityQuantity = activity.getAlreadyActivityQuantity(); //已经参与活动的订单数
                long activityQuantity = activity.getActivityQuantity(); //总共可以参与的订单数
                if (alreadyActivityQuantity>=activityQuantity){
                    isEnoughQuantity=true;
                }
            }
            if (current>=activityEndTime||isEnoughQuantity){
                try{
                    poushengGiftActivityWriteLogic.updatePoushengGiftActivityStatus(activity.getId(), PoushengGiftActivityEvent.HANDLE.toOrderOperation());
                }catch (Exception e){
                    log.error("update PoushengGiftActivity failed,id is {},caused by {}",activity.getId(),Throwables.getStackTraceAsString(e));
                }
            }
        }
        log.info("END JOB GiftActivityJob.doGiftActvity");
    }
}
