package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Mail: F@terminus.io
 * Data: 16/7/19
 * Author: yangzefeng
 */
@Component
@Slf4j
public class OrderWriteLogic {

    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private EventBus eventBus;


    public boolean updateOrder(OrderBase orderBase, OrderLevel orderLevel, MiddleOrderEvent orderEvent) {
        Flow flow = flowPicker.pickOrder();
        Integer targetStatus = flow.target(orderBase.getStatus(), orderEvent.toOrderOperation());

        switch (orderLevel) {
            case SHOP:
                Response<Boolean> updateShopOrderResp = orderWriteService.shopOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateShopOrderResp.isSuccess()) {
                    log.error("fail to update shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus, updateShopOrderResp.getError());
                    throw new JsonResponseException(updateShopOrderResp.getError());
                }
                return updateShopOrderResp.getResult();
            case SKU:
                Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(orderBase.getId(), orderBase.getStatus(), targetStatus);
                if (!updateSkuOrderResp.isSuccess()) {
                    log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                            orderBase.getId(), orderBase.getStatus(), targetStatus);
                    throw new JsonResponseException(updateSkuOrderResp.getError());
                }
                return updateSkuOrderResp.getResult();
            default:
                throw new IllegalArgumentException("unknown.order.type");
        }
    }

    /**
     * 更新子单已处理数量
     * @param skuOrderIdAndQuantity 子单id及数量
     */
    public void updateSkuHandleNumber(Map<Long,Integer> skuOrderIdAndQuantity){

        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        Flow flow = flowPicker.pickOrder();

        for (SkuOrder skuOrder : skuOrders){
            //1. 更新extra中剩余待处理数量
            Response<Integer> handleRes = updateSkuOrderExtra(skuOrder,skuOrderIdAndQuantity);
            //2. 判断是否需要更新子单状态
            if(handleRes.isSuccess()){
                //如果剩余数量为0则更新子单状态为待发货
                if(handleRes.getResult()==0){
                    Integer targetStatus = flow.target(skuOrder.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation());
                    Response<Boolean> updateSkuOrderResp = orderWriteService.skuOrderStatusChanged(skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                    if (!updateSkuOrderResp.isSuccess()) {
                        log.error("fail to update sku shop order(id={}) from current status:{} to target:{},cause:{}",
                                skuOrder.getId(), skuOrder.getStatus(), targetStatus);
                        throw new ServiceException(updateSkuOrderResp.getError());
                    }

                }
            }
        }


    }



    private Response<Integer> updateSkuOrderExtra(SkuOrder skuOrder,Map<Long,Integer> skuOrderIdAndQuantity){
        Map<String,String> extraMap = skuOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("sku order(id:{}) extra is null,can not update wait handle number reduce：{}",skuOrder.getId(),skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("");
        }
        if(!extraMap.containsKey(TradeConstants.WAIT_HANDLE_NUMBER)){
            log.error("sku order(id:{}) extra not contains key:{},can not update wait handle number reduce：{}",skuOrder.getId(),TradeConstants.WAIT_HANDLE_NUMBER,skuOrderIdAndQuantity.get(skuOrder.getId()));
            return Response.fail("");
        }
        Integer waitHandleNumber = Integer.valueOf(extraMap.get(TradeConstants.WAIT_HANDLE_NUMBER));
        if(waitHandleNumber<=0){
            log.error("sku order(id:{}) extra wait handle number:{} ,not enough to ship",skuOrder.getId(),waitHandleNumber);
            return Response.fail("");
        }
        Integer quantity = skuOrderIdAndQuantity.get(skuOrder.getId());
        Integer remainNumber = waitHandleNumber - quantity;
        if(remainNumber<0){
            log.error("sku order(id:{}) extra wait handle number:{} ship applyQuantity:{} ,not enough to ship",skuOrder.getId(),waitHandleNumber,quantity);
            return Response.fail("");
        }
        extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER,String.valueOf(remainNumber));
        Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,extraMap);
        if(!response.isSuccess()){
            log.error("update sku order：{} extra map to:{} fail,error:{}",skuOrder.getId(),extraMap,response.getError());
            return Response.fail("");
        }

        return Response.ok(remainNumber);
    }
}
