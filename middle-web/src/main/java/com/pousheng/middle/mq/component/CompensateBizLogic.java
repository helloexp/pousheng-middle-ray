package com.pousheng.middle.mq.component;

import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
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


    /**
     * 消息推送
     * @param compensateBiz
     */
    public long createBizAndSendMq(PoushengCompensateBiz compensateBiz,String topic){
        if (log.isDebugEnabled()){
            log.debug("CompensateBizLogic createBizAndSendMq,compensateBiz {}",compensateBiz);
        }
        Response<Long> result =  poushengCompensateBizWriteService.create(compensateBiz);
        if (result.isSuccess()){
            rocketMqProducerService.sendMessage(topic, JsonMapper.nonEmptyMapper().toJson(result.getResult()));
        }
        return result.getResult();
    }
}
