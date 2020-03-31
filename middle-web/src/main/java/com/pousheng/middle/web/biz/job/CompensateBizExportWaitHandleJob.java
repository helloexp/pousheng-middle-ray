package com.pousheng.middle.web.biz.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.controller.BizOperationClient;
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
import java.util.Map;
import java.util.Objects;

/**
 * 中台业务处理-处理从未处理过的任务的job,导入导出
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@ConditionalOnProperty(name = "biz.export.wait.handle.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@RestController
public class CompensateBizExportWaitHandleJob {
    @Autowired
    private PoushengCompensateBizReadService compensateBizReadService;
    @Autowired
    private CompensateBizProcessor compensateBizProcessor;
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private BizOperationClient operationClient;



    @Scheduled(cron = "0 */7 * * * ?")
    @GetMapping("/api/compensate/biz/export/wait/handle/job")
    public void processWaitHandleJob() {
        log.info("[pousheng-middle-compensate-biz-wait-handle-job] start...");

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
            criteria.setIgnoreCnt(3);
            criteria.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            Response<Paging<PoushengCompensateBiz>> response = compensateBizReadService.paging(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<PoushengCompensateBiz> compensateBizs =  response.getResult().getData();
            if (compensateBizs.isEmpty()){
                break;
            }
            log.info("wait handle compensateBizs size is {}",compensateBizs.size());
            for (PoushengCompensateBiz compensateBiz:compensateBizs){
                if (compensateBiz.getCnt()>3){
                    continue;
                }
                if (!Objects.equals(compensateBiz.getBizType(),PoushengCompensateBizType.EXPORT_TRADE_BILL.name())){
                    log.warn("file compensate biz not right ,id {},bizType {}",compensateBiz.getId(),compensateBiz.getBizType());
                    continue;
                }
                Map<String,Object> params = Maps.newHashMap();
                params.put("id",compensateBiz.getId());
                params.put("currentStatus",compensateBiz.getStatus());
                params.put("newStatus",PoushengCompensateBizStatus.PROCESSING.name());
                //乐观锁控制更新为处理中
                Response<Boolean> rU=  operationClient.put("api/biz/update/status",params);
                if (!rU.isSuccess()){
                    continue;
                }
                //业务处理
                compensateBizLogic.process(compensateBiz);

            }
            pageNo++;

        }
        stopwatch.stop();
        log.info("[pousheng-middle-biz-wait-handle-job] end");
    }





}
