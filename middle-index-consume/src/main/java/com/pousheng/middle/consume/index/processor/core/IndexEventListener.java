package com.pousheng.middle.consume.index.processor.core;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 14:14<br/>
 */
@Slf4j
public class IndexEventListener implements MessageListenerConcurrently {
    private Gson gson = new Gson();
    private List<IndexEventProcessorWrap> processors;

    public IndexEventListener(List<IndexEventProcessorWrap> processors) {
        this.processors = processors;
    }

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            Stopwatch sw = null;
            String payload = new String(msg.getBody());
            IndexEvent event = gson.fromJson(payload, IndexEvent.class);

            log.info("[INDEX-EVENT-LISTENER] on new index message, id: {}, table: {}, task: {}, size: {}", msg.getMsgId(), event.getTable(), event.getTaskName(), event.getData().size());
            if (log.isDebugEnabled()) {
                sw = Stopwatch.createStarted();
                log.debug("[INDEX-EVENT-LISTENER] on new index message: {}", payload);
            }

            onEvent(event);

            if (log.isDebugEnabled()) {
                log.debug("[INDEX-EVENT-LISTENER] index message consumed: id: {}, cost: {}", msg.getMsgId(), sw.elapsed(TimeUnit.MILLISECONDS));
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private void onEvent(IndexEvent event) {
        for (IndexEventProcessorWrap processor : processors) {
            if (processor.accept(event)) {
                log.info("[INDEX-EVENT-LISTENER] handle event by processor: {}:{}", processor.getTable(), processor.getTasks());
                processor.getProcessor().process(event);
            }
        }
    }
}
