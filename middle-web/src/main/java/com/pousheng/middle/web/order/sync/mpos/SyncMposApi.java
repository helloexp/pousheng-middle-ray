package com.pousheng.middle.web.order.sync.mpos;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * 同步mpos
 */
@Component
@Slf4j
public class SyncMposApi {

    //@Value("mpos.gateway")
    private String mposGateway = "http://api-test-mpos.pousheng.com";

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
        String gateWay = mposGateway + "/api/order/sync/shipment/express";
        String responseBody = HttpRequest.post(gateWay,param,true)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("response:{}",responseBody);
        return responseBody;
    }

//    /**
//     * 同步订单状态到mpos
//     * @param orderId
//     * @param status
//     * @return
//     */
//    public String syncOrderStatusToMpos(Long orderId,String status){
//        String gateWay = mposGateway + "";
//        log.info("sync orderId:{} status:{}",orderId,status);
//        Map<String,Serializable> param = Maps.newHashMapWithExpectedSize(2);
//        param.put("orderId",orderId);
//        param.put("status",status);
//        String responseBody = HttpRequest.put(gateWay,param,false)
//                .connectTimeout(10000).readTimeout(10000)
//                .body();
//        log.info("response:{}",responseBody);
//        return responseBody;
//    }
        public static void main(String[] args) {
            SyncMposApi api = new SyncMposApi();
            Map<String,Serializable> param = Maps.newHashMap();
//            param.put("orderId",0000);
//            param.put("id",000);
//            param.put("name","test");
//            param.put("shipmentType",1);
//            param.put("outShipmentId",0000);
//            param.put("outerSkuCodes","");
//            System.out.println(api.syncShipmentToMpos(param));
            param.put("shipmentId",000);
            param.put("shipmentCorpCode","test");
            param.put("shipmentSerialNo","test");
            param.put("shipmentDate","201712282213");
            System.out.println(api.syncShipmentShippedToMpos(param));
        }
}