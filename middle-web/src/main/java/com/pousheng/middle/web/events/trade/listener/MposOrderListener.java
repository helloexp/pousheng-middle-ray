package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.web.events.trade.MposOrderUpdateEvent;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * Created by penghui on 2017/12/22
 * mpos订单事件监听器
 */
public class MposOrderListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    /**
     * 订单状态更新
     * @param event
     */
    @Subscribe
    public void onMposOrderConfirmed(MposOrderUpdateEvent event){
        //syncMposOrderLogic.syncOrderStatus(event.getOrderId(),event.getType());
    }


}
