package com.pousheng.middle.mq.consumer;

import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.starter.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.starter.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


/**
 * 通用业务处理，12个线程
 * CompenSateBiz mq处理
 */
@ConditionalOnProperty(name = "biz.export.topic.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Service
@RocketMQMessageListener(topic = MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC,
        consumerGroup = MqConstants.POUSHENG_MIDDLE_MQ_EXPORT_CONSUMER_GROUP,consumeThreadMax = 12)
public class CompensateExportBizConsumerMessageListener implements RocketMQListener<String> {
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private CompensateBizProcessor compensateBizProcessor;
    @Override
    public void onMessage(String message) {
        PoushengCompensateBiz poushengCompensateBiz = null;
        try{
            if (log.isDebugEnabled()){
                log.debug("CompensateBizConsumerMessageListener onMessage,message {}",message);
            }
            //获取bizId，为什么不把整个bean拿过来，这边还要查一遍，因为biz的bean可能很大，太大了的话对于网络传输有影响，可能导致mq挂掉
            String compensateBizId = JsonMapper.nonEmptyMapper().fromJson(message,String.class);

            //查询biz任务
            Response<PoushengCompensateBiz> compensateBizResponse =  poushengCompensateBizReadService.findById(Long.valueOf(compensateBizId));
            if (!compensateBizResponse.isSuccess()){
                log.error("find comepensate biz failed,compenstateBizId {},caused by {}",compensateBizId,compensateBizResponse.getError());
                throw new BizException(compensateBizResponse.getError());
            }
            poushengCompensateBiz = compensateBizResponse.getResult();
            //乐观锁控制更新为处理中，幂等处理，假如有重复的消息过来的话，这边的状态会控制
            Response<Boolean> rU=  poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.WAIT_HANDLE.name(), PoushengCompensateBizStatus.PROCESSING.name());
            if (!rU.isSuccess()){
                log.warn("update compensate biz status failed, bizId {}",poushengCompensateBiz.getId());
                return;
            }
            //业务处理
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
}
