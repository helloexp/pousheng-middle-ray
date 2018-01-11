package com.pousheng.middle.web.order.sync.mpos;

import com.github.kevinsawicki.http.HttpRequest;
import io.terminus.open.client.parana.component.ParanaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * created by penghui on 2017/12/30
 * 同步mpos
 */
@Component
@Slf4j
public class SyncMposApi {

    @Autowired
    private ParanaClient paranaClient;

    /**
     * 同步发货单到mpos
     * @param param 参数
     * @return
     */
    public String syncShipmentToMpos(Map<String,Object> param){
        log.info("sync shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post("mpos.order.ship.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 只有恒康发货才通知mpos状态更新
     * @param param 参数
     * @return
     */
    public String syncShipmentShippedToMpos(Map<String,Object> param){
        log.info("sync shipment shipped to mpos,param:{}",param);
        String responseBody = paranaClient.post("mpos.order.ship.express",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 商品派不出去，同步mpos
     * @param param 参数
     * @return
     */
    public String syncNotDispatcherSkuToMpos(Map<String,Object> param){
        log.info("sync shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post("",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }
}