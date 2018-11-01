package com.pousheng.middle.web.biz.job;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 中台业务处理---处理失败失败的任务job
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@ConditionalOnProperty(name = "biz.failed.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@RestController
public class CompensateBizFailedJob {
    @Autowired
    private PoushengCompensateBizReadService compensateBizReadService;
    @Autowired
    private PoushengCompensateBizWriteService compensateBizWriteService;
    @Autowired
    private CompensateBizProcessor compensateBizProcessor;
    @Autowired
    private HostLeader hostLeader;

    @Scheduled(cron = "0 */3 * * * ?")
    @GetMapping("/api/compensate/biz/failed/job")
    public void processFailedJob() {
        log.info("[pousheng-middle-compensate-biz-failed-job] start...");
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        Integer pageNo = 1;
        Integer pageSize = 100;
        while (true) {
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setStatus(PoushengCompensateBizStatus.FAILED.name());
            criteria.setIgnoreCnt(3);
            Response<Paging<PoushengCompensateBiz>> response = compensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<PoushengCompensateBiz> compensateBizs = response.getResult().getData();
            if (compensateBizs.isEmpty()) {
                break;
            }
            log.info("wait handle compensateBizs size is {}",compensateBizs.size());
            //轮询业务处理
            for (PoushengCompensateBiz compensateBiz : compensateBizs) {
                if (compensateBiz.getCnt()>3){
                    continue;
                }
                //乐观锁控制更新为处理中
                Response<Boolean> rU = compensateBizWriteService.updateStatus(compensateBiz.getId(), compensateBiz.getStatus(), PoushengCompensateBizStatus.PROCESSING.name());
                if (!rU.isSuccess()) {
                    continue;
                }
                //业务处理
                try{
                    compensateBizProcessor.doProcess(compensateBiz);
                    compensateBizWriteService.updateStatus(compensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.SUCCESS.name());
                }catch (BizException e0){
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", compensateBiz.getId(), compensateBiz.getBizType(), e0.getMessage());
                    compensateBizWriteService.updateStatus(compensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                    compensateBizWriteService.updateLastFailedReason(compensateBiz.getId(),e0.getMessage(),(compensateBiz.getCnt()+1));
                }catch (Exception e1){
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", compensateBiz.getId(), compensateBiz.getBizType(), e1.getMessage());
                    compensateBizWriteService.updateStatus(compensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                    compensateBizWriteService.updateLastFailedReason(compensateBiz.getId(),e1.getMessage(),(compensateBiz.getCnt()+1));
                }
            }
            pageNo++;

        }
        stopwatch.stop();
        log.info("[pousheng-middle-compensate-biz-failed-job] end");
    }
}
