package com.pousheng.middle.web.events.warehouse;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.erp.component.MaterialPusher;
import io.terminus.open.client.center.event.OpenClientPushItemSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 监听中台推送外部渠道事件
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-25
 */
@Component
@Slf4j
public class OpenClientPushItemSuccessEventListener {
    private final EventBus eventBus;

    private final MaterialPusher materialPusher;

    @Autowired
    public OpenClientPushItemSuccessEventListener(EventBus eventBus, MaterialPusher materialPusher) {
        this.eventBus = eventBus;
        this.materialPusher = materialPusher;
    }

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(OpenClientPushItemSuccessEvent event){
        //向库存那边推送这个信息, 表示要关注这个商品对应的单据
        materialPusher.addSpus(Lists.newArrayList(event.getItemId()));
    }
}
