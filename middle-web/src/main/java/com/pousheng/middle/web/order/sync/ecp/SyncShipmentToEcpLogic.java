package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 同步电商发货单逻辑
 * Created by tony on 2017/7/5.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncShipmentToEcpLogic {
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    /**
     * 同步发货单号到电商平台
     * @param shipment 发货单
     * @return 同步结果,result中有快递公司,单号信息
     */
    public Response<Boolean> syncShipmentToECP(Shipment shipment)
    {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment,orderOperation);
        if(!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步电商
        Shipment newStatusShipment = shipmentReadLogic.findShipmentById(shipment.getId());
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(newStatusShipment, syncOrderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("shipment(id:{}) operation :{} fail,error:{}",shipment.getId(),syncOrderOperation.getText(),updateSyncStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }
            return Response.ok();
    }
}
