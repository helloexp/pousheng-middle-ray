package com.pousheng.middle.web.events.trade.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.web.events.trade.UnLockStockEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 锁定库存事件:适用于销售发货单创建
 * Created by tony on 2017/7/13.
 * pousheng-middle
 */
@Slf4j
@Component
public class UnLockStockListener {
    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private StockPusher stockPusher;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void doUnLockStock(UnLockStockEvent event){

        Shipment shipment = event.getShipment();

        //获取发货仓信息
        ShipmentExtra extra = shipmentReadLogic.getShipmentExtra(shipment);

        Response<Boolean> unlockRlt =  mposSkuStockLogic.unLockStock(shipment);
        if (!unlockRlt.isSuccess()){
            log.error("this shipment can not unlock stock,shipment id is :{},warehouse id is:{}",shipment.getId(),extra.getWarehouseId());
        }


    }

}
