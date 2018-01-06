package com.pousheng.middle.open.mpos;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.events.trade.MposOrderUpdateEvent;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.order.component.*;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2017/12/25
 */
@RestController
@Slf4j
@RequestMapping("/api/mpos")
public class MposOpenApi {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private OrderWriteService orderWriteService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    /**
     *  接收mpos发货单状态更新
     *  修改中台发货单状态
     * @param shipmentId 发货单id
     * @param status 状态
     * @param extra 扩展信息
     * @return
     */
    @ApiOperation("发货单状态更新")
    @RequestMapping(value = "/shipment/{shipmentId}/update",method = RequestMethod.PUT)
    public Response<Boolean> updateOuterShipment(@PathVariable Long shipmentId, @RequestParam String status, @RequestParam(required = false) Map<String,String> extra){
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Map<String, String> extraMap = shipment.getExtra();
        MiddleOrderEvent orderEvent;
        Shipment update = null;
        switch (status){
            case TradeConstants.MPOS_SHIPMENT_WAIT_SHIP:
                orderEvent = MiddleOrderEvent.MPOS_RECEIVE;
                break;
            case TradeConstants.MPOS_SHIPMENT_REJECT:
                orderEvent = MiddleOrderEvent.MPOS_REJECT;

                break;
            case TradeConstants.MPOS_SHIPMENT_SHIPPED:
                orderEvent = MiddleOrderEvent.SHIP;
                update = new Shipment();
                update.setId(shipment.getId());
                //保存物流信息
                shipmentExtra.setShipmentSerialNo(extra.get("shipmentSerialNo"));
                shipmentExtra.setShipmentCorpCode(extra.get("shipmentCorpCode"));
                ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(extra.get("shipmentCorpCode"));
                shipmentExtra.setShipmentCorpName(expressCode.getName());
                DateTime dt = DateTime.parse(extra.get("shipmentDate"), DFT);
                shipmentExtra.setShipmentDate(dt.toDate());
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                update.setExtra(extraMap);
                break;
            default:
                throw new JsonResponseException("illegal type");
        }
        if(Objects.nonNull(update))
            shipmentWiteLogic.update(update);
        Response<Boolean> res = shipmentWiteLogic.updateStatus(shipment,orderEvent.toOrderOperation());
        if(!res.isSuccess()){
            log.error("sync shipment(id:{}) fail,cause:{}",shipmentId,res.getError());
            throw new JsonResponseException(res.getError());
        }
        eventBus.post(new MposShipmentUpdateEvent(shipmentId,orderEvent));
        log.info("sync shipment(id:{}) success",shipmentId);
        return Response.ok(true);
    }

    /**
     *  确认收货
     * @param shopOrderId 订单ID
     * @return
     */
    @ApiOperation("确认收货")
    @RequestMapping(value = "/order/{shopOrderId}/confirm",method = RequestMethod.PUT)
    public Response<Boolean> confirmedOuterOrder(@PathVariable Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(), MiddleOrderStatus.SHIPPED.getValue());
        if (skuOrders.size() == 0) {
            log.error("sku order not allow confirm");
            throw new JsonResponseException("shop.order.confirm.fail");
        }
        for (SkuOrder skuOrder : skuOrders) {
            Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), MiddleOrderStatus.SHIPPED.getValue(), MiddleOrderStatus.CONFIRMED.getValue());
            if (!updateRlt.getResult()) {
                log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                throw new JsonResponseException("shop.order.confirm.fail");
            }
        }
        //判断订单的状态是否是已完成
        ShopOrder shopOrder1 = orderReadLogic.findShopOrderById(shopOrderId);
        if (!Objects.equals(shopOrder1.getStatus(), MiddleOrderStatus.CONFIRMED.getValue())) {
            log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order",
                    shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
            throw new JsonResponseException("shop.order.already.confirmed");
        } else {
            //更新同步电商状态为已确认收货
            OrderOperation successOperation = MiddleOrderEvent.CONFIRM.toOrderOperation();
            Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            if (response.isSuccess()) {
                //通知恒康发货单收货时间
                NotifyHkOrderDoneEvent event = new NotifyHkOrderDoneEvent();
                event.setShopOrderId(shopOrder.getId());
                eventBus.post(event);
            }
        }
        return Response.ok(true);
    }

    /**
     * 取消订单/发货单
     * @param id      id
     * @param type    类型 order 订单, shipment 发货单
     * @return
     */
    @ApiOperation("取消订单/发货单")
    @RequestMapping(value = "/order/{id}/cancel")
    public Response<Boolean> cancelOuterOrder(@PathVariable Long id,@RequestParam String type){
        if(Objects.equals(TradeConstants.ORDER_CANCEL,type)){
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(id);
            Flow orderFlow = flowPicker.pickOrder();
            if(orderFlow.operationAllowed(shopOrder.getStatus(), MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation())){
                log.error("cancel order(id:{}) fail,the order shipped",id);
                throw new JsonResponseException("shop.order.cancel.fail");
            }
            List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(id);
            if(CollectionUtils.isEmpty(shipments)){
                log.error("order(id:{}) has no shipment");
                throw new JsonResponseException("shop.order.cancel.fail");
            }
            List<Shipment> filterShipments = shipments.stream().filter(Objects::nonNull).
                    filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(),MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
            for(Shipment shipment:filterShipments){
                if(!orderFlow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_SHIP.toOrderOperation()) && !orderFlow.operationAllowed(shipment.getStatus(),MiddleOrderEvent.CANCEL_HK.toOrderOperation())){
                    log.error("shipment(id:{}) shipped");
                    throw new JsonResponseException("shop.order.cancel.fail");
                }
            }
            eventBus.post(new MposOrderUpdateEvent(id,MiddleOrderStatus.CANCEL.getValue(),filterShipments));
        }
        if(Objects.equals(TradeConstants.SHIPMENT_CANCEL,type)){
            Shipment shipment = shipmentReadLogic.findShipmentById(id);
            if(shipment.getStatus() > MiddleShipmentsStatus.WAIT_SHIP.getValue() || shipment.getStatus() < MiddleShipmentsStatus.WAIT_SYNC_HK.getValue()){
                log.error("cancel shipment(id:{}) fail,the shipment shipped",id);
                throw new JsonResponseException("shop.shipment.cancel.fail");
            }
            eventBus.post(new MposShipmentUpdateEvent(id,MiddleOrderEvent.CANCEL));
        }
        return Response.ok(true);
    }
}
