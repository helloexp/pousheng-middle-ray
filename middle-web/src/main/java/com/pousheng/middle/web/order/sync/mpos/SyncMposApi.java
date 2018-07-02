package com.pousheng.middle.web.order.sync.mpos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * created by penghui on 2017/12/30
 * 同步mpos
 */
@Component
@Slf4j
public class SyncMposApi {

    @Autowired
    private MiddleParanaClient paranaClient;

    @Value("${mpos.open.shop.id:180}")
    private Long shopId;

    /**
     * 同步发货单到mpos
     * @param param 参数
     * @return
     */
    public String syncShipmentToMpos(Map<String,Object> param){
        log.info("sync shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.order.ship.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 同步全渠道订单的发货单到mpos
     * @param param
     * @return
     */
    public String syncAllChannelShipmnetToMpos(Map<String,Object> param){
        log.info("sync all-channel-order shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"sync.order.ship.api",param);
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
        log.info("sync not dispatcher sku to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.reject.afterSales",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 拉取mpos发货单状态
     * @param param
     * @return
     */
    public String syncShipmentStatus(Map<String,Object> param){
        log.info("sync shipments status from mpos,param:{}",param);
        String responseBody = paranaClient.get(shopId,"mpos.query.ship.status",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 恒康收到退货后，通知mpos退款
     * @param param
     * @return
     */
    public String syncRefundReceive(Map<String,Object> param){
        log.info("sync shipments status from mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"mpos.seller.confirm.afterSales",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    public String revokeMposShipment(Map<String,Object> param){
        log.info("revoke shipments for mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"sync.mposShipment.cancel.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    public String revokeNewMposShipment(Map<String,Object> param){
        log.info("revoke shipments for mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"omni.shipment.cancel.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }
    /**
     * 同步new全渠道订单的发货单到mpos
     * @param param
     * @return
     */
    public String syncNewAllChannelShipmnetToMpos(Map<String,Object> param){
        log.info("sync all-channel-order shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"omni.shipment.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }

    /**
     * 第三方确认收货之后，将售后单同步通知到mpos
     * @param param
     * @return
     */
    public String omniShipmmentConfirm(Map<String,Object> param){
        log.info("sync all-channel-order shipments to mpos,param:{}",param);
        String responseBody = paranaClient.post(shopId,"omni.shipment.confirm.api",param);
        log.info("response:{}",responseBody);
        return responseBody;
    }
}
