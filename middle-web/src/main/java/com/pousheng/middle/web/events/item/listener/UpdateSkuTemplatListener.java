package com.pousheng.middle.web.events.item.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.item.service.SkuTemplateSearchWriteService;
import com.pousheng.middle.web.events.item.SkuTemplateUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Component
public class UpdateSkuTemplatListener {


    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private SkuTemplateSearchWriteService skuTemplateSearchWriteService;

    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    @Subscribe
    public void onUpdate(SkuTemplateUpdateEvent event){

        log.debug("update sku template to search index start");
        Long skuTemplateId = event.getSkuTemplateId();

        Response<Boolean> updateRes = skuTemplateSearchWriteService.index(skuTemplateId);
        if(!updateRes.isSuccess()){
            log.error("update sku template(id:{}) to search index fail,error:{}",skuTemplateId,updateRes.getError());
        }

        log.debug("update sku template to search index  end");

    }


}
