package com.pousheng.middle.web.order.sync.hk;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 同步恒康发货单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentLogic {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    /**
     * 同步发货单到恒康
     * @param shipment 发货单
     * @return 同步结果 result 为恒康的发货单号
     */
    public Response<Boolean> syncShipmentToHk(Shipment shipment){
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("shipment(id:{}) operation :{} fail,error:{}",shipment.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步恒康，同步调用成功后，更新发货单的状态，及冗余恒康发货单号

        return Response.ok();

    }

    /**
     * 同步发货单取消到恒康
     * @param shipment 发货单
     * @return 同步结果
     */
    public Response<Boolean> syncShipmentCancelToHk(Shipment shipment){

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("shipment(id:{}) operation :{} fail,error:{}",shipment.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步恒康，同步调用成功后，更新发货单的状态

        return Response.ok(Boolean.TRUE);

    }

}
