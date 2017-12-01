package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.events.trade.StepOrderNotifyHkEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/30
 * pousheng-middle
 */
@Slf4j
@Component
public class StepOrderNotifyHkListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;


    @PostConstruct
    public void init() {
        eventBus.register(this);
    }
    @Subscribe
    public void updateEcpOrderInitialStatus(StepOrderNotifyHkEvent event) {
        Long shopOrderId = event.getShopOrderId();
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Map<String, String> extraMap = shopOrder.getExtra();
        extraMap.put(TradeConstants.STEP_ORDER_STATUS, String.valueOf(OpenClientStepOrderStatus.PAID.getValue()));
        Response<Boolean> r = orderWriteService.updateOrderExtra(shopOrderId, OrderLevel.SHOP, extraMap);
        if (!r.isSuccess()) {
            log.error("update shopOrder extra failed, shopOrder id is {},caused by {}", shopOrderId, r.getError());
            return;
        }
        //同步发货单到恒康
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        //获取待同步恒康的发货单
        List<OrderShipment> orderShipmentFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(orderShipment -> Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.WAIT_SYNC_HK.getValue()))
                .collect(Collectors.toList());
        //将待同步恒康的发货单发到恒康
        for (OrderShipment orderShipment : orderShipmentFilter) {
            try {
                Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
                log.info("auto create shipment,step xxx");
                if (!syncRes.isSuccess()) {
                    log.error("sync shipment(id:{}) to hk fail,error:{}", shipment.getId(), syncRes.getError());
                }
            } catch (Exception e) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", orderShipment.getShipmentId(), e.getMessage());
            }
        }

    }
}
