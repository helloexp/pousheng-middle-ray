package com.pousheng.middle.web.order.sync.hk;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 同步恒康发货单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentLogic {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    /**
     * 同步发货单到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentToHk(Shipment shipment) {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步恒康
        Shipment newStatusShipment = shipmentReadLogic.findShipmentById(shipment.getId());
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(newStatusShipment, syncOrderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //冗余恒康发货单号
        Shipment update = new Shipment();
        update.setId(shipment.getId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        shipmentExtra.setOutShipmentId(shipment.getId().toString());
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shipmentExtra));
        update.setExtra(extraMap);
        shipmentWiteLogic.update(update);


        return Response.ok();

    }

    /**
     * 同步发货单取消到恒康
     *
     * @param shipment 发货单
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentCancelToHk(Shipment shipment) {

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步恒康，同步调用成功后，更新发货单的状态
        if (true) {
            OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
            Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
            if (!updateStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                return Response.fail(updateStatusRes.getError());
            }
        } else {
            OrderOperation orderOperation1 = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
            Response<Boolean> updateStatusRes1 = shipmentWiteLogic.updateStatus(shipment, orderOperation1);
            if (!updateStatusRes1.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation1.getText(), updateStatusRes1.getError());
                return Response.fail(updateStatusRes.getError());
            }
            return Response.fail("sync.cancel.hk.failed");
        }
        return Response.ok(Boolean.TRUE);

    }

}
