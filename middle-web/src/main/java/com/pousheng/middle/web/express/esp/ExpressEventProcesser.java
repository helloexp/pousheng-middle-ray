package com.pousheng.middle.web.express.esp;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.model.PoushengEspLog;
import com.pousheng.middle.order.service.PoushengEspLogService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Desc oxo发货信息处理
 * @Author GuoFeng
 * @Date 2019/9/4
 */
@Slf4j
@Component
public class ExpressEventProcesser {

//    @Autowired
//    private ExpressProcessService expressProcessService;
//
//    @Autowired
//    private ShipmentReadService shipmentReadService;
//
//    @Autowired
//    private OrderShipmentReadService orderShipmentReadService;
//
//    @Autowired
//    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private PoushengEspLogService poushengEspLogService;

    @Autowired
    private EventBus eventBus;


    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

//    /**
//     * 处理oxo仓库发货单发货，回传发货信息至esp
//     *
//     * @param event
//     */
//    @Subscribe
//    public void processWarehouseShipmentShipedEvent(WarehouseShipmentShipedEvent event) {
//        Response<Shipment> shipmentResponse = shipmentReadService.findById(event.getShipmentId());
//        if (shipmentResponse.isSuccess()) {
//            Shipment shipment = shipmentResponse.getResult();
//            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
//            if (orderShipmentResponse.isSuccess()) {
//                OrderShipment orderShipment = orderShipmentResponse.getResult();
//                Long orderId = orderShipment.getOrderId();
//                Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(orderId);
//                if (shopOrderResponse.isSuccess()) {
//                    ShopOrder shopOrder = shopOrderResponse.getResult();
//                    ESPExpressCodeSendResponse espExpressCodeSendResponse = expressProcessService.sendWarehouseExpressNo(shopOrder, shipment, event.getExpresscode(), event.getExpressbillno());
//                    log.info("处理仓发回传，返回信息:{}", espExpressCodeSendResponse);
//                }
//            }
//        }
//    }
//
//    /**
//     * 处理oxo店发发货单，回传至esp
//     *
//     * @param event
//     */
//    @Subscribe
//    public void processStoreShipmentShipedEvent(StoreShipmentShipedEvent event) {
//        Response<Shipment> shipmentResponse = shipmentReadService.findById(event.getShipmentId());
//        if (shipmentResponse.isSuccess()) {
//            Shipment shipment = shipmentResponse.getResult();
//            Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
//            if (orderShipmentResponse.isSuccess()) {
//                OrderShipment orderShipment = orderShipmentResponse.getResult();
//                Long orderId = orderShipment.getOrderId();
//                Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(orderId);
//                if (shopOrderResponse.isSuccess()) {
//                    ShopOrder shopOrder = shopOrderResponse.getResult();
//                    JSONObject espExpressCodeSendResponse = expressProcessService.sendExpressNo(shopOrder, shipment, event.getExpresscode(), event.getExpressbillno());
//                    log.info("处理店发回传，返回信息:{}", espExpressCodeSendResponse);
//                }
//            }
//        }
//
//    }


    /**
     * 回传日志记录
     *
     * @param log
     */
    @Subscribe
    public void writeExpressSendLog(PoushengEspLog log) {
        Response<PoushengEspLog> poushengEspLogByShipmentCode = poushengEspLogService.findPoushengEspLogByShipmentCode(log.getMiddleShipmentNo());
        if (poushengEspLogByShipmentCode.isSuccess()) {
            PoushengEspLog espLog = poushengEspLogByShipmentCode.getResult();
            if (espLog != null) {
                Integer retryNum = espLog.getRetryNum();
                log.setRetryNum(retryNum + 1);
                log.setId(espLog.getId());
                poushengEspLogService.updatePoushengEspLog(log);
                return;
            }
        }
        poushengEspLogService.createPoushengEspLog(log);
    }
}
