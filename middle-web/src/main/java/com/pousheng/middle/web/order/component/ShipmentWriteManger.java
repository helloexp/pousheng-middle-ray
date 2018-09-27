package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.enums.StockRecordType;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import com.pousheng.middle.web.events.warehouse.StockRecordEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 生成发货单的操作：防止出现并发的情况
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/6
 * pousheng-middle
 */
@Component
@Slf4j
public class ShipmentWriteManger {

    @Autowired
    private ShipmentWriteService shipmentWriteService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private MiddleShipmentWriteService middleShipmentWriteService;

    @Transactional
    public Long createShipmentByConcurrent(Shipment shipment, ShopOrder shopOrder, Boolean withSafe){
        log.info("begin to create shipment,shopOrderId is {}",shopOrder.getId());
        //1.生成发货单
        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrder.getId()), OrderLevel.SHOP);
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, shopOrder.getId(), OrderLevel.SHOP.getValue(), createResp.getError());
            throw new ServiceException(createResp.getError());
        }
        //2.子订单扣减数量
        this.decreaseSkuOrderWaitHandleNumber(shipment);
        log.info("end to create shipment,shipmentId is {}", createResp.getResult());

        // 锁库存
        Response<Boolean> rDecrease = mposSkuStockLogic.lockStock(shipment, withSafe);
        if(!rDecrease.isSuccess()){
            log.error("failed to decreaseStocks, shipment id: {}, error code:{},auto dispatch stock failed", createResp.getResult(), rDecrease.getError());
            throw new ServiceException(rDecrease.getError());
        }

        return createResp.getResult();
    }

    public void decreaseSkuOrderWaitHandleNumber(Shipment shipment) {
        Map<Long, Integer> skuOrderIdAndQuantity = shipment.getSkuInfos();
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        for (Long skuOrderId : skuOrderIds) {
            Response<Boolean> response = orderWriteService.decreaseWaitHandleNumberAndShipmentCode(skuOrderId, skuOrderIdAndQuantity.get(skuOrderId),shipment.getShipmentCode());
            if (!response.isSuccess()) {
                log.error("decrease sku order with hold failed,skuOrderId is {},caused by {}", skuOrderId, response.getError());
                throw new ServiceException(response.getError());
            }
        }
    }

    public void rollbackSkuOrderWaitHandleNumber(Shipment shipment){
        Map<Long, Integer> skuOrderIdAndQuantity = shipment.getSkuInfos();
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        for (Long skuOrderId : skuOrderIds) {
            Response<Boolean> response = orderWriteService.rollbackWaitHandleNumber(skuOrderId, skuOrderIdAndQuantity.get(skuOrderId));
            if (!response.isSuccess()) {
                log.error("decrease sku order with hold failed,skuOrderId is {},caused by {}", skuOrderId, response.getError());
                throw new ServiceException(response.getError());
            }
        }
    }


    @Transactional
    public Long createForAfterSale(Shipment shipment, OrderRefund orderRefund, Long afterSaleOrderIdr) {
        Response<Long> createResp = middleShipmentWriteService.createForAfterSale(shipment, orderRefund, afterSaleOrderIdr);
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for afterSaleOrderIdr(id={}),and level={},cause:{}",
                    shipment, afterSaleOrderIdr, OrderLevel.SHOP.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }

        log.info("end to create shipment,shipmentId is {}", createResp.getResult());

        // 锁库存
        Response<Boolean> rDecrease = mposSkuStockLogic.lockStock(shipment, Boolean.TRUE);
        if(!rDecrease.isSuccess()) {
            log.error("failed to decreaseStocks, shipment id: {}, error code:{},auto dispatch stock failed", createResp.getResult(), rDecrease.getError());
            throw new ServiceException(rDecrease.getError());
        }
        return createResp.getResult();
    }

}
