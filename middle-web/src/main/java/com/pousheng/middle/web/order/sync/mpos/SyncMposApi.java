package com.pousheng.middle.web.order.sync.mpos;

import com.github.kevinsawicki.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
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

    //@Value("mpos.gateway")
    private String mposGateway = "http://api-test-mpos.pousheng.com";

    //private String mposGateway = "http://30.40.86.253:8089";

    /**
     * 同步发货单到mpos
     * @param param 参数
     * @return
     */
    public String syncShipmentToMpos(Map<String,Serializable> param){
        log.info("sync shipments to mpos,param:{}",param);
        String gateway = mposGateway + "/api/order/sync/shipment";
        String responseBody = HttpRequest.post(gateway,param,true)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 只有恒康发货才通知mpos状态更新
     * @param param 参数
     * @return
     */
    public String syncShipmentShippedToMpos(Map<String,Serializable> param){
        log.info("sync shipment shipped to mpos,param:{}",param);
        String gateWay = mposGateway + "/api/order/sync/shipment/express";
        String responseBody = HttpRequest.put(gateWay,param,true)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 商品派不出去或连续被拒单，同步mpos
     * @param param 参数
     * @return
     */
    public String syncAfterSaleToMpos(Map<String,Serializable> param){
        log.info("sync shipments to mpos,param:{}",param);
        String gateway = mposGateway + "/api/order/sync";
        String responseBody = HttpRequest.post(gateway,param,true)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 针对退货情况，仓库收货通知mpos
     * @return
     */
    public String syncWarehouseConfirmToMpos(Map<String,Serializable> param){
        log.info("sync shipments to mpos,param:{}",param);
        String gateway = mposGateway + "/api/order";
        String responseBody = HttpRequest.put(gateway,param,true)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("response:{}",responseBody);
        return responseBody;
    }
}