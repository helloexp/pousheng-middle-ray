package com.pousheng.middle.web.order.component;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.events.trade.UnLockStockEvent;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
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
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    public Response<Boolean> updateStatus(Shipment shipment, OrderOperation orderOperation) {

        Flow flow = flowPicker.pickShipments();
        if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
            log.error("shipment(id:{}) current status:{} not allow operation:{}", shipment.getId(), shipment.getStatus(), orderOperation.getText());
            return Response.fail("shipment.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
        Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
        if (!updateRes.isSuccess()) {
            log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(), updateRes.getError());
            return Response.fail(updateRes.getError());
        }
        shipment.setStatus(targetStatus);
        return Response.ok();

    }


    //更新发货单
    public void update(Shipment shipment) {
        Response<Boolean> updateRes = shipmentWriteService.update(shipment);
        if (!updateRes.isSuccess()) {
            log.error("update shipment:{} fail,error:{}", shipment, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    //更新发货单Extra
    public void updateExtra(Long shipmentId, Map<String, String> extraMap) {

        Shipment updateShipment = new Shipment();
        updateShipment.setId(shipmentId);
        updateShipment.setExtra(extraMap);

        this.update(updateShipment);
    }

    /**
     * 取消发货单逻辑
     *
     * @param shipment
     * @return 取消成功 返回返回true,取消失败返回false
     */
    public boolean cancelShipment(Shipment shipment) {
        try {
            Flow flow = flowPicker.pickShipments();
            //未同步恒康,现在只需要将发货单状态置为已取消即可
            if (flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_SHIP.toOrderOperation())) {
                Response<Boolean> cancelRes = this.updateStatus(shipment, MiddleOrderEvent.CANCEL_SHIP.toOrderOperation());
                if (!cancelRes.isSuccess()) {
                    log.error("cancel shipment(id:{}) fail,error:{}", shipment.getId(), cancelRes.getError());
                    throw new JsonResponseException(cancelRes.getError());
                }
                //解锁库存
                UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
                unLockStockEvent.setShipment(shipment);
                eventBus.post(unLockStockEvent);
            }
            //已经同步过恒康,现在需要取消同步恒康,根据恒康返回的结果判断是否取消成功
            if (flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentCancelToHk(shipment);
                if(!syncRes.isSuccess()){
                    log.error("sync cancel shipment(id:{}) to hk fail,error:{}",shipment.getId(),syncRes.getError());
                    throw new JsonResponseException(syncRes.getError());
                }
                //解锁库存
                UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
                unLockStockEvent.setShipment(shipment);
                eventBus.post(unLockStockEvent);
            }
            return true;
        } catch (Exception e) {
            log.error("");
            return false;
        }


    }
}
