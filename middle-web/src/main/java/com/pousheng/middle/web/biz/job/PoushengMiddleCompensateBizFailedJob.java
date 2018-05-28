package com.pousheng.middle.web.biz.job;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.PoushengMiddleCompensateBizProcessor;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
@RestController
public class PoushengMiddleCompensateBizFailedJob {
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private PoushengMiddleCompensateBizProcessor poushengMiddleCompensateBizProcessor;

    @Scheduled(cron = "0 */1 * * * ?")
    @GetMapping("/api/compensate/biz/failed/job")
    public void processFailedJob() {
        log.info("[pousheng-middle-compensate-biz-failed-job] start...");
        Stopwatch stopwatch = Stopwatch.createStarted();
        Integer pageNo = 1;
        Integer pageSize = 100;
        while (true) {
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setStatus(PoushengCompensateBizStatus.FAILED.name());
            Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<PoushengCompensateBiz> poushengCompensateBizs = response.getResult().getData();
            if (poushengCompensateBizs.isEmpty()) {
                break;
            }
            //轮询业务处理
            for (PoushengCompensateBiz poushengCompensateBiz : poushengCompensateBizs) {
                if (poushengCompensateBiz.getCnt() > 3) {
                    continue;
                }
                //乐观锁控制更新为处理中
                Response<Boolean> rU = poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), poushengCompensateBiz.getStatus(), PoushengCompensateBizStatus.PROCESSING.name());
                if (!rU.isSuccess()) {
                    continue;
                }
                //业务处理
                Response<Boolean> processResponse = poushengMiddleCompensateBizProcessor.doProcess(PoushengCompensateBizType.valueOf(poushengCompensateBiz.getBizType()), poushengCompensateBiz);
                if (!processResponse.isSuccess()) {
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType(), processResponse.getError());
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                } else {
                    poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.SUCCESS.name());
                }
            }
            pageNo++;

        }
        stopwatch.stop();
        log.info("[pousheng-middle-compensate-biz-failed-job] end");
    }
}
