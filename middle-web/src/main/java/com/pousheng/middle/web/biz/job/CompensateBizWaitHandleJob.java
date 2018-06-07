package com.pousheng.middle.web.biz.job;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 中台业务处理-处理从未处理过的任务的job
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@Slf4j
@RestController
public class CompensateBizWaitHandleJob {
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private CompensateBizProcessor compensateBizProcessor;

    @Scheduled(cron = "0 */1 * * * ?")
    @GetMapping("/api/compensate/biz/wait/handle/job")
    public void processWaitHandleJob() {
        log.info("[pousheng-middle-compensate-biz-wait-handle-job] start...");
        Stopwatch stopwatch = Stopwatch.createStarted();
        Integer pageNo = 1;
        Integer pageSize = 100;
        while (true) {
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<PoushengCompensateBiz> poushengCompensateBizs =  response.getResult().getData();
            if (poushengCompensateBizs.isEmpty()){
                break;
            }
            for (PoushengCompensateBiz poushengCompensateBiz:poushengCompensateBizs){
                if (poushengCompensateBiz.getCnt()>3){
                    continue;
                }
                //乐观锁控制更新为处理中
                Response<Boolean> rU=  poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),poushengCompensateBiz.getStatus(),PoushengCompensateBizStatus.PROCESSING.name());
                if (!rU.isSuccess()){
                    continue;
                }
                //业务处理
                try{
                    compensateBizProcessor.doProcess(poushengCompensateBiz);
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.SUCCESS.name());
                }catch (BizException e0){
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}",poushengCompensateBiz.getId(),poushengCompensateBiz.getBizType(),e0);
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());
                    poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(),e0.getMessage(),(poushengCompensateBiz.getCnt()+1));
                }catch (Exception e1){
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}",poushengCompensateBiz.getId(),poushengCompensateBiz.getBizType(),e1);
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());
                    poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(),e1.getMessage(),(poushengCompensateBiz.getCnt()+1));
                }

            }
            pageNo++;

        }
        stopwatch.stop();
        log.info("[pousheng-middle-compensate-biz-wait-handle-job] end");
    }
}
