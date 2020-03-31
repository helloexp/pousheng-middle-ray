package com.pousheng.middle.web.yintai.mq;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.yintai.YintaiConstant;
import com.pousheng.middle.web.yintai.component.MiddleYintaiItemService;
import com.pousheng.middle.web.yintai.dto.MessageDTO;
import com.pousheng.middle.web.yintai.dto.YintaiPushItemDTO;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
@ConditionalOnProperty(name = "yintai.item.push.consumer.enable", havingValue = "true", matchIfMissing = false)
@Slf4j
@Component
@MQConsumer
public class YintaiMessageCustomer {

    @Autowired
    private MiddleYintaiItemService middleYintaiItemService;
    @Autowired
    @Qualifier(value = "yintaiEventBus")
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @MQSubscribe(topic = YintaiConstant.YINTAI_ITEM_PUSH_TOPIC, consumerGroup = YintaiConstant.YINTAI_ITEM_PUSH_GROUP, consumeMode = ConsumeMode.CONCURRENTLY)
    public void processMsg(String jsonMessage) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        MessageDTO messageDTO = JSON.parseObject(jsonMessage, MessageDTO.class);

        if (!Strings.isNullOrEmpty(messageDTO.getSkuCode())) {
            //sku纬度处理
            eventBus.post(messageDTO.getSkuCode());
            return;
        }
        //品牌纬度处理
        List<YintaiPushItemDTO> pushItems = middleYintaiItemService.findPushItems(messageDTO.getBrandMapping().getBrandId(), messageDTO.getStart());
        stopwatch.stop();
        log.info("find push item result size({}), brand({}), startTime({}) cost time:({})", pushItems.size(),messageDTO.getBrandMapping().getBrandId(),messageDTO.getStart(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        if (pushItems.isEmpty()) {
            return;
        }

        eventBus.post(pushItems);

    }

    @Subscribe
    public void doUploadHandle(List<YintaiPushItemDTO> pushItems) {
        log.info("[yintaiEvent] doUploadHandle start ");
        middleYintaiItemService.uploadHandle(pushItems);
        log.info("[yintaiEvent] doUploadHandle end ");
    }

    @Subscribe
    public void doUploadHandle(String skuCode) {
        log.info("[yintaiEvent] doUploadHandle by skuCode start ");
        middleYintaiItemService.uploadBySkuCodes(Lists.newArrayList(skuCode));
        log.info("[yintaiEvent] doUploadHandle by skuCode end ");
    }
}
