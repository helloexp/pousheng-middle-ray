package com.pousheng.middle.web.events.trade.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.web.events.trade.MposShipmentCreateEvent;
import com.pousheng.middle.web.events.trade.MposShipmentUpdateEvent;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by penghui on 2017/12/22
 * 发货单事件监听器
 */
@Component
@Slf4j
public class MposShipmentListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private OrderWriteService orderWriteService;

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;

    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @Autowired
    private SyncShipmentLogic syncShipmentLogic;

    @Autowired
    private SyncMposShipmentLogic syncMposShipmentLogic;

    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    /**
     * 判断是否所有发货单都更新了 更新订单状态
     * @param event
     */
    @Subscribe
    public void onUpdateMposShipment(MposShipmentUpdateEvent event){
        Shipment shipment = shipmentReadLogic.findShipmentById(event.getShipmentId());
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.MPOS_RECEIVE){
            this.syncOrderStatus(shipment,MiddleShipmentsStatus.WAIT_SHIP.getValue(),MiddleOrderStatus.WAIT_SHIP.getValue());
        }
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.SHIP){
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if(Objects.equals(shipmentExtra.getShipmentWay(), TradeConstants.MPOS_SHOP_DELIVER)){
                //扣减库存
                DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
                mposSkuStockLogic.decreaseStock(dispatchOrderItemInfo);
                // 发货推送pos信息给恒康
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                if(!response.isSuccess()){
                    Map<String,Object> param = Maps.newHashMap();
                    param.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
                }
            }
            this.syncOrderStatus(shipment,MiddleShipmentsStatus.SHIPPED.getValue(),MiddleOrderStatus.SHIPPED.getValue());
        }
        if(event.getMiddleOrderEvent() == MiddleOrderEvent.MPOS_REJECT){
            //解锁库存
            DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
            mposSkuStockLogic.unLockStock(dispatchOrderItemInfo);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            List<SkuCodeAndQuantity> skuCodeAndQuantities = shipmentReadLogic.findShipmentSkuDetail(shipment);
            shipmentWiteLogic.toDispatchOrder(shopOrder,skuCodeAndQuantities);
        }
    }

    /**
     * 同步发货单
     * @param event
     */
    @Subscribe
    public void onCreateMposShipment(MposShipmentCreateEvent event){
        Shipment shipment = event.getShipment();
        Integer type = event.getType();
        if(Objects.equals(type,1)){
            //发货单同步恒康
            log.info("sync shipment(id:{}) to hk",shipment.getId());
            Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipment);
            if (!syncRes.isSuccess()) {
                log.error("sync shipment(id:{}) to hk fail,error:{}", shipment.getId(), syncRes.getError());
            }
        }else if(Objects.equals(type,2)){
            //同步mpos
            log.info("sync shipment(id:{}) to mpos", shipment.getId());
            Response response = syncMposShipmentLogic.syncShipmentToMpos(shipment);
            if (!response.isSuccess()) {
                log.error("sync shipment(id:{}) to mpos fail", shipment.getId());
            }
        }else{
            ShopOrder shopOrder = event.getShopOrder();
            log.info("sync order(id:{}) not dispatch sku to mpos...",shopOrder.getId());
            syncMposOrderLogic.syncNotDispatcherSkuToMpos(shopOrder,event.getSkuCodeAndQuantities());
        }
    }

    /**
     * 同步订单状态
     * @param shipment      发货单
     * @param targetStatus  发货单状态
     * @param expectOrderStatus   期望订单状态
     */
    private void syncOrderStatus(Shipment shipment,Integer targetStatus,Integer expectOrderStatus){
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        long orderShopId = orderShipment.getOrderId();
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
        //获取该订单下所有的orderShipment信息
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(orderShopId);
        //过滤掉已经取消的发货单
        List<OrderShipment> orderShipmentsFilter = orderShipments.stream().filter(Objects::nonNull)
                .filter(it->!Objects.equals(MiddleShipmentsStatus.CANCELED.getValue(),it.getStatus()) && !Objects.equals(MiddleShipmentsStatus.REJECTED.getValue(),it.getStatus())).collect(Collectors.toList());
        //获取发货单的状态
        List<Integer> orderShipmentStatusList = Lists.transform(orderShipmentsFilter, new Function<OrderShipment, Integer>() {
            @Nullable
            @Override
            public Integer apply(@Nullable OrderShipment orderShipment) {
                return orderShipment.getStatus();
            }
        });
        //判断订单是否已经全部更新状态了
        int count=0;
        for (Integer status:orderShipmentStatusList){
            if (!Objects.equals(status,targetStatus)){
                count++;
            }
        }
        if (count==0) {
            //更新订单状态
            List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(orderShopId, shopOrder.getStatus());
            for (SkuOrder skuOrder : skuOrders) {
                Response<Boolean> updateRlt = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), expectOrderStatus);
                if (!updateRlt.getResult()) {
                    log.error("update skuOrder status error (id:{}),original status is {}", skuOrder.getId(), skuOrder.getStatus());
                    throw new JsonResponseException("update.sku.order.status.error");
                }
            }
            //如果订单状态变成已发货，同步ecpstatus
            if(Objects.equals(expectOrderStatus,MiddleOrderStatus.SHIPPED.getValue())){
                OrderOperation successOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
            }
        }
    }

}