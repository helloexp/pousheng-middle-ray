package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ShopOrder;
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
public class SyncOrderToEcpLogic {
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderServiceCenter orderServiceCenter;

    /**
     * 同步发货单到电商
     * @param shopOrder
     * @param expressCompayCode
     * @param shipmentId
     * @return
     */
    public Response<Boolean> syncOrderToECP(ShopOrder shopOrder,String expressCompayCode,Long shipmentId)
    {
        //更新状态为同步中
        try {
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, orderOperation);
            OpenClientOrderShipment orderShipment = new OpenClientOrderShipment();
            orderShipment.setOuterOrderId(shopOrder.getOutId());
            orderShipment.setLogisticsCompany(expressCompayCode);
            orderShipment.setWaybill(String.valueOf(shipmentId));
            Response<Boolean> response = orderServiceCenter.ship(shopOrder.getShopId(), orderShipment);
            if (response.isSuccess()) {
                //同步成功
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            } else {
                //同步失败
                OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
                return Response.fail("sync.ecp.fail");
            }
        }catch (Exception e) {
            log.error("sync ecp failed,shopOrderId is({}),cause by {}", shopOrder.getId(), e.getMessage());
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder, failOperation);
        }

        return Response.ok();
    }
}
