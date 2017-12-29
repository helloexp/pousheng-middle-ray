package com.pousheng.middle.web.order.sync.mpos;

import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 同步mpos订单状态
 * 1. 待接单 待发货 待收货 已收货
 */
@Component
@Slf4j
public class SyncMposOrderLogic {

    @Autowired
    private SyncMposApi syncMposApi;

//    public Boolean syncOrderStatus(Long orderId,String status){
//        Response res = JsonMapper.nonDefaultMapper().fromJson(syncMposApi.syncOrderStatusToMpos(orderId,status),Response.class);
//        if(!res.isSuccess()){
//            log.error("sync orderId:{} status:{} fail,cause:{}",orderId,status,res.getError());
//            return false;
//        }
//        log.info("sync orderId:{} status:{} success",orderId,status);
//        return true;
//    }

}
