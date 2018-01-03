package com.pousheng.middle.open.mpos;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private WarehouseSkuWriteService warehouseSkuWriteService;
    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;

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
                //todo 接单推送绩效店铺给恒康
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
                //扣减库存
                this.decreaseStock(shipment);
                break;
            default:
                return Response.fail("illegal status");
        }
        if(Objects.nonNull(update))
            shipmentWiteLogic.update(update);
        Response<Boolean> res = shipmentWiteLogic.updateStatus(shipment,orderEvent.toOrderOperation());
        if(!res.isSuccess()){
            log.error("sync shipment(id:{}) fail,cause:{}",shipmentId,res.getError());
            return Response.fail(res.getError());
        }
        eventBus.post(new MposShipmentUpdateEvent(shipmentId,orderEvent));
        log.info("sync shipment(id:{}) success",shipmentId);
        return Response.ok(true);
    }

    /**
     *  修改本地订单状态
     * @param shopOrderId 订单ID
     * @return
     */
    @ApiOperation("确认收货")
    @RequestMapping(value = "/order/{shopOrderId}/confirm",method = RequestMethod.PUT)
    public Response<Boolean> confirmedOuterOrder(@PathVariable Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Boolean res = orderWriteLogic.updateOrder(shopOrder, OrderLevel.SHOP,MiddleOrderEvent.CONFIRM);
        if(!res){
            log.error("sync order(id:{}) fail",shopOrderId);
            return Response.fail("sync order(id:{}) fail");
        }
        List<OrderShipment> orderShipmentList = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        for(OrderShipment orderShipment:orderShipmentList){
            Shipment shipment = shipmentReadLogic.findShipmentById(orderShipment.getShipmentId());
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if(Objects.equals(shipmentExtra.getShipmentWay(),"2")){
//            todo 通知恒康已经收货
//            Response<Boolean> response= syncShipmentLogic.syncShipmentDoneToHk(shipment,2, MiddleOrderEvent.AUTO_HK_CONFIRME_FAILED.toOrderOperation());
//            if (!response.isSuccess()){
//                log.error("notify hk order confirm failed,shipment id is ({}),caused by {}",shipment.getId(),response.getError());
//            }
            }
            Response<Boolean> response = shipmentWiteLogic.updateStatus(shipment,MiddleOrderEvent.CONFIRM.toOrderOperation());
            if (!response.isSuccess()){
                log.error("sync shipment(id:{}) confirm fail",shipment.getId());
                return Response.fail(response.getError());
            }
        }
        orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.CONFIRM.toOrderOperation());
        return Response.ok(true);
    }

    /**
     * 扣减库存
     * @param shipment 发货单
     */
    private void decreaseStock(Shipment shipment){
        //获取发货单下的sku订单信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);
        List<WarehouseShipment> warehouseShipmentList = Lists.newArrayList();
        WarehouseShipment warehouseShipment = new WarehouseShipment();
        //组装sku订单数量信息
        List<SkuCodeAndQuantity> skuCodeAndQuantities =makeSkuCodeAndQuantities(shipmentItems);
        warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
        warehouseShipment.setWarehouseId(extra.getWarehouseId());
        warehouseShipment.setWarehouseName(extra.getWarehouseName());
        warehouseShipmentList.add(warehouseShipment);
        Response<Boolean> decreaseStockRlt =  warehouseSkuWriteService.decreaseStock(warehouseShipmentList,warehouseShipmentList);
        if (!decreaseStockRlt.isSuccess()){
            log.error("this shipment can not decrease stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }
        //触发库存推送
        List<String> skuCodes = Lists.newArrayList();
        for (WarehouseShipment ws : warehouseShipmentList) {
            for (SkuCodeAndQuantity skuCodeAndQuantity : ws.getSkuCodeAndQuantities()) {
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }
        stockPusher.submit(skuCodes);
    }

    private List<SkuCodeAndQuantity> makeSkuCodeAndQuantities(List<ShipmentItem> list){
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayList();
        if (list.size()>0){
            for (ShipmentItem shipmentItem:list){
                SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
                skuCodeAndQuantity.setSkuCode(shipmentItem.getSkuCode());
                skuCodeAndQuantity.setQuantity(shipmentItem.getQuantity());
                skuCodeAndQuantities.add(skuCodeAndQuantity);
            }
        }
        return skuCodeAndQuantities;
    }
}
