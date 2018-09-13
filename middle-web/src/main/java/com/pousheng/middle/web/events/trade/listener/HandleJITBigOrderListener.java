package com.pousheng.middle.web.events.trade.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.biz.job.CompensateBizWaitHandleJobNew;
import com.pousheng.middle.web.events.trade.HandleJITBigOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 处理JIT 大订单的事件监听
 * @author tanlongjun
 */
@ConditionalOnBean(CompensateBizWaitHandleJobNew.class)
@Slf4j
@Component
public class HandleJITBigOrderListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private CompensateBizWaitHandleJobNew compensateBizWaitHandleJobNew;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(HandleJITBigOrderEvent event) {
        try {
            // 手动消费 可能会跟定时触发消费 处理的是同一个任务
            // 故在处理同一个任务的时候 需要加锁互斥 避免重复处理
            List<Long> ids = Lists.newArrayList(event.getId());

            compensateBizWaitHandleJobNew.immediateConsume(ids);
        }catch (Exception e){
            log.error("failed to handle jit big order event.task id :{}",event.getId(),e);
        }
    }

}
