package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
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

    /**
     * 同步订单到电商平台
     * @param shopOrder
     * @return
     */
    public Response<Boolean> syncOrderToECP(ShopOrder shopOrder)
    {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
        orderWriteLogic.updateEcpOrderStatus(shopOrder,orderOperation);

        //todo 同步电商
        if (true){
            //同步成功
            OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder,successOperation);
        }else{
            OrderOperation failOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            orderWriteLogic.updateEcpOrderStatus(shopOrder,failOperation);
        }
        return Response.ok();
    }
}
