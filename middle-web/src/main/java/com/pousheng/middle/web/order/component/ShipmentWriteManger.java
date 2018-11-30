package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.*;
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
        shipment.setShipmentCode("SHP"+ createResp.getResult());
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
            Response<Boolean> response = orderWriteService.rollbackWaitHandleNumber(skuOrderId, skuOrderIdAndQuantity.get(skuOrderId), shipment.getShipmentCode());
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

    /**
     * 保存纯邮费商品发货单
     * @param shipment
     * @param shopOrder
     * @return
     */
    @Transactional
    public Long createPostageShipment(Shipment shipment, ShopOrder shopOrder,List<SkuOrder> skuOrders){
        log.info("begin to create postage shipment,shopOrderId is {}",shopOrder.getId());
        //1.生成发货单
        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrder.getId()), OrderLevel.SHOP);
        if (!createResp.isSuccess()) {
            log.error("fail to create postage shipment:{} for order(id={}),and level={},cause:{}",
                shipment, shopOrder.getId(), OrderLevel.SHOP.getValue(), createResp.getError());
            throw new ServiceException(createResp.getError());
        }
        //2.子订单扣减数量
        shipment.setShipmentCode("SHP"+ createResp.getResult());
        this.decreaseSkuOrderWaitHandleNumber(shipment);
        log.info("end to create postage shipment,shipmentId is {}", createResp.getResult());

        //3.更新订单状态
        orderWriteService.updateOrderStatus(shopOrder.getId(),OrderLevel.SHOP, MiddleOrderStatus.SHIPPED.getValue());

        //4.更新子单状态
        for(SkuOrder skuOrder:skuOrders) {
            orderWriteService.updateShippedNum(skuOrder.getId(),skuOrder.getQuantity());
            orderWriteService.updateOrderStatus(skuOrder.getId(), OrderLevel.SKU,
                MiddleOrderStatus.SHIPPED.getValue());
        }
        return createResp.getResult();
    }

}
