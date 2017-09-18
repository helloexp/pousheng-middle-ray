package com.pousheng.middle.open;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.ych.logger.events.TOPCallEvent;
import io.terminus.open.client.center.job.order.api.OrderSearchNotifier;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.shop.model.OpenShop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by cp on 9/18/17.
 */
@Component
public class PsOrderSearchNotifier implements OrderSearchNotifier {

    @Autowired
    private EventBus eventBus;

    @Override
    public void notify(OpenShop openShop) {
        if (OpenClientChannel.from(openShop.getChannel()) == OpenClientChannel.TAOBAO) {
            eventBus.post(new TOPCallEvent("/api/system"));
        }
    }
}
