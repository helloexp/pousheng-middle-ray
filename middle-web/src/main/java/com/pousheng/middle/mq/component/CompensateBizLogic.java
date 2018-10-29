package com.pousheng.middle.mq.component;

import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.mq.component
 * 2018/10/28 16:54
 * pousheng-middle
 */
@Component
@Slf4j
public class CompensateBizLogic {
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private RocketMqProducerService rocketMqProducerService;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private CompensateBizProcessor compensateBizProcessor;


    /**
     * 消息推送
     * @param compensateBiz
     */
    public long createBizAndSendMq(PoushengCompensateBiz compensateBiz,String topic){
        if (log.isDebugEnabled()){
            log.debug("CompensateBizLogic createBizAndSendMq,compensateBiz {}",compensateBiz);
        }
        Response<Long> resultRes =  poushengCompensateBizWriteService.create(compensateBiz);
        if (resultRes.isSuccess()){
            log.info("create biz success and send mq topic:{} message:{}",topic,resultRes.getResult());
            rocketMqProducerService.sendMessage(topic, JsonMapper.nonEmptyMapper().toJson(resultRes.getResult()));
        } else {
            log.error("create biz:{} fail,error:{}",compensateBiz,resultRes.getError());
            throw new JsonResponseException(resultRes.getError());
        }
        return resultRes.getResult();
    }





    public void consumeMqMessage(String message){
        PoushengCompensateBiz poushengCompensateBiz = null;
        try{
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
            Response<Boolean> rU=  poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(), PoushengCompensateBizStatus.WAIT_HANDLE.name(), PoushengCompensateBizStatus.PROCESSING.name());
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
