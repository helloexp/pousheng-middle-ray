package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.erp.component.SpuImporter;
import io.terminus.open.client.center.event.OpenClientPullBarCodeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 中台根据skuCode拉取ERP货品信息
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/4
 * pousheng-middle
 */
@Slf4j
@Component
public class MiddlePullMaterialsListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SpuImporter spuImporter;
    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    @Subscribe
    public void doneShipment(OpenClientPullBarCodeEvent event) {
        log.info("try to pull spus from erp ,where skucCode is {}",event.getSkuCode());

        String skuCode =  event.getSkuCode();

        int spuCount = spuImporter.processPullMarterials(skuCode);

        log.info("synchronized spus by skuCode {},resultCount is {}",skuCode, spuCount);
    }
}
