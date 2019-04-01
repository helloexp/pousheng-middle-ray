package com.pousheng.middle.mq.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizProcessor;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.controller.BizOperationClient;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

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
    @Autowired
    private BizOperationClient operationClient;

    @Value("${common.biz.mq.queue.size:0}")
    public Integer commonBizMqQueueSize;


    /**
     * 消息推送
     * @param compensateBiz
     */
    public long createBizAndSendMq(PoushengCompensateBiz compensateBiz,String topic){
        if (log.isDebugEnabled()){
            log.debug("CompensateBizLogic createBizAndSendMq,compensateBiz {}",compensateBiz);
        }
        Response<Long> resultRes =  poushengCompensateBizWriteService.create(compensateBiz);
        // queueSize 为0则默认取实际的队列数。
        int queueSize = 0;
        // 如果是通用补偿的biz Topic 则用配置的队列大小。已达到限定指定队列范围的目的(0到queueSize范围)
        if (Objects.equals(topic, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC)) {
            queueSize = commonBizMqQueueSize;
        }
        if (resultRes.isSuccess()){
            log.info("create biz success and send mq topic:{} message:{}",topic,resultRes.getResult());
            rocketMqProducerService.asyncSendOrderly(topic, JsonMapper.nonEmptyMapper().toJson(resultRes.getResult())
                , compensateBiz.getId(), queueSize);
        } else {
            log.error("create biz:{} fail,error:{}",compensateBiz,resultRes.getError());
            throw new JsonResponseException(resultRes.getError());
        }
        return resultRes.getResult();
    }

    /**
     * 指定队列的消息推送
     * @param compensateBiz
     * @param topic
     * @param queueIndex
     * @return
     */
    public long createBizAndSendMq(PoushengCompensateBiz compensateBiz,String topic,int queueIndex){
        if (log.isDebugEnabled()){
            log.debug("CompensateBizLogic createBizAndSendMq,compensateBiz {}",compensateBiz);
        }
        Response<Long> resultRes =  poushengCompensateBizWriteService.create(compensateBiz);
        if (resultRes.isSuccess()){
            log.info("create biz success and send mq topic:{} message:{}",topic,resultRes.getResult());
            rocketMqProducerService.asyncSendOrderly(topic, JsonMapper.nonEmptyMapper().toJson(resultRes.getResult()),queueIndex);
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
        } catch (BizException e0){
            log.error("process pousheng biz failed,id is {},bizType is {},caused by {}",poushengCompensateBiz.getId(),poushengCompensateBiz.getBizType(),e0);
            poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());
            poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(),e0.getMessage(),(poushengCompensateBiz.getCnt()+1));
        } catch (Exception e1){
            log.error("process pousheng biz failed,id is {},bizType is {},caused by {}",poushengCompensateBiz.getId(),poushengCompensateBiz.getBizType(),e1);
            poushengCompensateBizWriteService.updateStatus(poushengCompensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());
            poushengCompensateBizWriteService.updateLastFailedReason(poushengCompensateBiz.getId(),e1.getMessage(),(poushengCompensateBiz.getCnt()+1));
        }
    }


    /**
     * mq 消费
     * @param message message
     */
    public void consumeExportMqMessage(String message){
        // 延迟30秒消费防止 数据库主从同步存在时间差
        try {
            Thread.sleep(30000);
        } catch (Exception e) {}

        PoushengCompensateBiz poushengCompensateBiz = null;
            //获取bizId，为什么不把整个bean拿过来，这边还要查一遍，因为biz的bean可能很大，太大了的话对于网络传输有影响，可能导致mq挂掉
        String compensateBizId = JsonMapper.nonEmptyMapper().fromJson(message,String.class);

        //查询biz任务
        Response<PoushengCompensateBiz> compensateBizResponse =  poushengCompensateBizReadService.findById(Long.valueOf(compensateBizId));
        if (!compensateBizResponse.isSuccess()){
            log.error("not find comepensate biz failed,compenstateBizId {},error by {}",compensateBizId,compensateBizResponse.getError());
            //throw new BizException(compensateBizResponse.getError());
            //查询失败则直接返回（由于导出读取的从库，从库与主库数据同步可能会存在时间差）
            return;
        }
        poushengCompensateBiz = compensateBizResponse.getResult();
        //乐观锁控制更新为处理中，幂等处理，假如有重复的消息过来的话，这边的状态会控制

        if (Objects.isNull(poushengCompensateBiz)) {
            //此处不抛异常 让消息消费成功 不更新失败而浪费一次可失败的次数。让其走Job轮训补偿
            return;
        }
        Map<String,Object> params = Maps.newHashMap();
        params.put("id",poushengCompensateBiz.getId());
        params.put("currentStatus",PoushengCompensateBizStatus.WAIT_HANDLE.name());
        params.put("newStatus",PoushengCompensateBizStatus.PROCESSING.name());
        //乐观锁控制更新为处理中
        Response<Boolean> rU=  operationClient.put("api/biz/update/status",params);
        if (!rU.isSuccess()){
            log.warn("update compensate biz status failed, bizId {}",poushengCompensateBiz.getId());
            return;
        }
        //业务处理
        process(poushengCompensateBiz);


    }


    /**
     * 业务处理
     * @param compensateBiz biz
     */
    public void process(PoushengCompensateBiz compensateBiz) {

        try{
            compensateBizProcessor.doProcess(compensateBiz);
            updateSuccess(compensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.SUCCESS.name());

        } catch (BizException e0){
            log.error("process pousheng  biz failed,id is {},bizType is {},caused by {}",compensateBiz.getId(),compensateBiz.getBizType(),e0);

            updateSuccess(compensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());
            //失败次数直接设置为3，导出的失败不需要重试
            updateFail(compensateBiz.getId(),e0.getMessage(),3);

        } catch (Exception e1){

            log.error("process pousheng  biz failed,id is {},bizType is {},caused by {}",compensateBiz.getId(),compensateBiz.getBizType(),e1);

            updateSuccess(compensateBiz.getId(),PoushengCompensateBizStatus.PROCESSING.name(),PoushengCompensateBizStatus.FAILED.name());

            //失败次数直接设置为3，导出的失败不需要重试
            updateFail(compensateBiz.getId(),e1.getMessage(),3);
        }
    }


    private void updateFail(Long id,String reason,Integer count){
        Map<String,Object> params = Maps.newHashMap();
        params.put("id",id);
        params.put("failReason",reason);
        params.put("count",count);
        operationClient.put("api/biz/update/fail/reason",params);
    }


    private void updateSuccess(Long id,String currentStatus,String newStatus){
        Map<String,Object> params = Maps.newHashMap();
        params.put("id",id);
        params.put("currentStatus",currentStatus);
        params.put("newStatus",newStatus);
        operationClient.put("api/biz/update/status",params);
    }
}
