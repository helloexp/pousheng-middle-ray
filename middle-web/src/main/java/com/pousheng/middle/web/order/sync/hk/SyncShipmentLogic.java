package com.pousheng.middle.web.order.sync.hk;

import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 同步恒康发货单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentLogic {


    /**
     * 同步发货单到恒康
     * @param shipmentId 发货单id（order shipment 的id）
     * @return 同步结果 result 为恒康的发货单号
     */
    public Response<String> syncShipmentToHk(Long shipmentId){

        return Response.ok(new Random(1000).toString());

    }

    /**
     * 同步发货单取消到恒康
     * @param shipmentId 发货单id（order shipment 的id）
     * @return 同步结果
     */
    public Response<Boolean> syncShipmentCancelToHk(Long shipmentId){

        return Response.ok(Boolean.TRUE);

    }

}
