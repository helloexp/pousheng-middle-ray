package com.pousheng.middle.web.order.component;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 发货单写服务
 * Created by songrenfei on 2017/7/2
 */
@Component
@Slf4j
public class ShipmentWiteLogic {
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    public Response<Boolean> updateStatus(Shipment shipment, OrderOperation orderOperation){

        Flow flow = flowPicker.pickShipments();
        if(!flow.operationAllowed(shipment.getStatus(),orderOperation)){
            log.error("shipment(id:{}) current status:{} not allow operation:{}",shipment.getId(),shipment.getStatus(),orderOperation.getText());
            return Response.fail("shipment.status.invalid");
        }

        Integer targetStatus = flow.target(shipment.getStatus(),orderOperation);
        Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(),targetStatus);
        if(!updateRes.isSuccess()){
            log.error("update shipment(id:{}) status to:{} fail,error:{}",shipment.getId(),updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok();

    }


    //更新发货单
    public void update(Shipment shipment){
        Response<Boolean> updateRes =  shipmentWriteService.update(shipment);
        if(!updateRes.isSuccess()){
            log.error("update shipment:{} fail,error:{}",shipment,updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    //更新发货单Extra
    public void updateExtra(Long shipmentId, Map<String,String> extraMap){

        Shipment updateShipment = new Shipment();
        updateShipment.setId(shipmentId);
        updateShipment.setExtra(extraMap);

        this.update(updateShipment);
    }

}
