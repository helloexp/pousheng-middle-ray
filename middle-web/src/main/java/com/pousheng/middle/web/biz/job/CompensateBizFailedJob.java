package com.pousheng.middle.web.biz.job;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.utils.mail.MailLogic;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
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
    @Autowired
    private MailLogic mailLogic;
    @Value("${pousheng.order.email.remind.group}")
    private String[] mposEmailGroup;
    // 邮件发送开关
    @Value("${pousheng.msg.send}")
    private Boolean sendLock;


    @Scheduled(cron = "0 */7 * * * ?")
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
            //开始时间为当前时间减去1天
            criteria.setStartCreatedAt(DateTime.now().minusDays(1).toDate());
            //结束时间为当前时间
            criteria.setEndCreatedAt(DateTime.now().toDate());

            Response<Paging<Long>> response = compensateBizReadService.pagingIds(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<Long> compensateBizIds = response.getResult().getData();
            if (compensateBizIds.isEmpty()) {
                break;
            }
            log.info("wait handle compensateBizIds size is {}",compensateBizIds.size());
            //轮询业务处理
            for (Long compensateBizId : compensateBizIds) {

                Response<PoushengCompensateBiz> result = compensateBizReadService.findById(compensateBizId);
                if (!result.isSuccess()){
                    continue;
                }
                PoushengCompensateBiz compensateBiz = result.getResult();
                //乐观锁控制更新为处理中
                Response<Boolean> rU = compensateBizWriteService.updateStatus(compensateBiz.getId(), PoushengCompensateBizStatus.FAILED.name(), PoushengCompensateBizStatus.PROCESSING.name());
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
                    //失败超过三次添加预警邮件
                    if (compensateBiz.getCnt()+1>=3&&sendLock){
                        sendWarnEmails(compensateBiz);
                    }
                }catch (Exception e1){
                    log.error("process pousheng biz failed,id is {},bizType is {},caused by {}", compensateBiz.getId(), compensateBiz.getBizType(), e1.getMessage());
                    compensateBizWriteService.updateStatus(compensateBiz.getId(), PoushengCompensateBizStatus.PROCESSING.name(), PoushengCompensateBizStatus.FAILED.name());
                    compensateBizWriteService.updateLastFailedReason(compensateBiz.getId(),e1.getMessage(),(compensateBiz.getCnt()+1));
                    //失败超过三次添加预警邮件
                    if (compensateBiz.getCnt()+1>=3&&sendLock){
                        sendWarnEmails(compensateBiz);
                    }
                }
            }
            pageNo++;
        }
        stopwatch.stop();
        log.info("[pousheng-middle-compensate-biz-failed-job] end");
    }

    private void sendWarnEmails(PoushengCompensateBiz poushengCompensateBiz){
        List<String> list = Lists.newArrayList();
        list.addAll(Arrays.asList(mposEmailGroup));
        log.info("send biz process failed email to : {}", JsonMapper.nonEmptyMapper().toJson(list));
        mailLogic.sendMail(String.join(",", list),"中台biz任务处理失败",
                "中台biz任务处理异常:有一个类型为:"+poushengCompensateBiz.getBizType()+",任务id为:"+poushengCompensateBiz.getId()+"的任务处理异常,异常原因为:"
                        +poushengCompensateBiz.getLastFailedReason() + "，请立即处理");
        log.info("send email success");
    }
}
