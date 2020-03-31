package com.pousheng.middle.web.excel;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.task.dto.TaskDTO;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.pousheng.middle.web.excel.MQTaskConsumer.*;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-17 16:52<br/>
 */
@Slf4j
@Component
public class MQTaskProducer {
    private final TerminusMQProducer terminusMQProducer;

    public MQTaskProducer(TerminusMQProducer terminusMQProducer) {
        this.terminusMQProducer = terminusMQProducer;
    }


    public void sendStartMessage(TaskDTO message) {
        String payload = JsonMapper.nonEmptyMapper().toJson(message);
        if (!send(new UnifiedEvent("start", payload), message.getId().toString())) {
            throw new JsonResponseException("supply.rule.start.fail");
        }
    }

    public void sendStopMessage(Long id, Long timeoutSecs) {
        String payload = JsonMapper.nonEmptyMapper().toJson(ImmutableMap.of("id", id, "timeout", timeoutSecs));
        if (!send(new UnifiedEvent("stop", payload), id.toString())) {
            throw new JsonResponseException("supply.rule.stop.fail");
        }
    }

    public boolean send(UnifiedEvent event, String id) {
        log.info("sending message: {} to mq, message key: {}", event, id);
        SendResult result = terminusMQProducer.send(TOPIC, TAG, JsonMapper.nonEmptyMapper().toJson(event), id);
        if (Objects.equals(SendStatus.SEND_OK, result.getSendStatus())) {
            log.info("sending message success, message id: {}", result.getMsgId());
            return true;
        }
        log.error("failed to send message to mq, cause: {}", result.getSendStatus());
        return false;
    }
}
