package com.pousheng.middle.web.events.warehouse;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.open.client.center.event.OpenClientItemMappingDeleteEvent;
import io.terminus.open.client.center.event.OpenClientPushItemSuccessEvent;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;

/**
 * 监听中台推送外部渠道事件
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-25
 */
@Component
@Slf4j
public class OpenClientPushItemEventListener {
    private final EventBus eventBus;

    private final MaterialPusher materialPusher;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    public OpenClientPushItemEventListener(EventBus eventBus, MaterialPusher materialPusher) {
        this.eventBus = eventBus;
        this.materialPusher = materialPusher;
    }

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onAddEvent(OpenClientPushItemSuccessEvent event){
        Long openShopId = event.getOpenShopId();
        try{
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(openShopId);
            Map<String,String> openShopExtra = openShop.getExtra();
            String itemMappingStock = openShopExtra.get("itemMappingStock");
            //向库存那边推送这个信息, 表示要关注这个商品对应的单据
            materialPusher.addSpus(Lists.newArrayList(event.getItemId()));
            //调用恒康抓紧给我返回库存信息
            if (StringUtils.isEmpty(itemMappingStock)|| Objects.equals(itemMappingStock,"false")){
                log.info("itemMappingStock value is {}",itemMappingStock);
                materialPusher.pushItemForStock(event.getItemId());
            }

        }catch (Exception e){
            log.error("push item failed,caused by {}",e.getCause());
        }
    }

    @Subscribe
    public void onDeleteEvent(OpenClientItemMappingDeleteEvent event){
        //向库存那边推送这个信息, 表示不再关注这个商品对应的单据
        materialPusher.removeSpus(Lists.newArrayList(event.getItemId()));
    }
}
