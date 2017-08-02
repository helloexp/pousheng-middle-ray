package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.events.trade.HkShipmentDoneEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;

/**
 * 恒康第一个发货单发货完成之后需要将ecpOrderstatus状态从初始的代发货修改为已发货
 * Created by tony on 2017/7/13.
 * pousheng-middle
 */
@Slf4j
@Component
public class EcpOrderListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void updateEcpOrderInitialStatus(HkShipmentDoneEvent event) {

        Shipment shipment = event.getShipment();
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long orderShopId = orderShipment.getOrderId();
        //获取店铺订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
        //获取ecpOrderStatus
        String status = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS, shopOrder);
        //获取shipment的Extra信息
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        try {
            ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
            //判断ecpOrder的状态是否是初始的待发货状态,如果不是,跳过
            if (Objects.equals(Integer.valueOf(status), EcpOrderStatus.WAIT_SHIP.getValue())) {
                Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.SHIP.toOrderOperation());
                if (!response.isSuccess()) {
                    log.error("update shopOrder(id:{}) failed, error:{}", orderShopId, response.getError());
                    throw new ServiceException(response.getError());
                }
                //冗余shipmentId到extra中
                Map<String, String> extraMap1 = shopOrder.getExtra();
                extraMap1.put(TradeConstants.ECP_SHIPMENT_ID, String.valueOf(shipment.getId()));
                Response<Boolean> response1 = orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, extraMap1);
                if (!response1.isSuccess()) {
                    log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), response1.getError());
                    throw new ServiceException(response1.getError());
                }
                //第一个发货单发货完成之后需要将订单同步到电商
                String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(), expressCode);
                syncOrderToEcpLogic.syncOrderToECP(shopOrder, expressCompanyCode, shipment.getId());
            }
        } catch (ServiceException e) {
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), e.getMessage());
        }catch (Exception e) {
            log.error("update shopOrder：{}  failed,error:{}", shopOrder.getId(), e.getMessage());
        }

    }

}
