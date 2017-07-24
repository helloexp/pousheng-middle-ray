package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
@Component
@Slf4j
public class MiddleOrderManager {
    @Autowired
    private SkuOrderDao skuOrderDao;
    @Autowired
    private ShopOrderDao shopOrderDao;
    @RpcConsumer
    private OrderWriteService orderWriteService;


    /**
     * 更新总单与子单的状态
     *
     * @param shopOrder
     * @param skuOrders
     * @param orderOperation
     */
    @Transactional
    public void updateOrderStatusAnndSkuQuantities(ShopOrder shopOrder, List<SkuOrder> skuOrders, OrderOperation orderOperation) {
        Flow flow = this.pickOrder();
        //更新sku订单记录
        for (SkuOrder skuOrder : skuOrders) {
            if (!flow.operationAllowed(skuOrder.getStatus(), orderOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", skuOrder.getId(), skuOrder.getStatus(), orderOperation.getText());
                throw new JsonResponseException("order.status.invalid");
            }
            Integer targetStatus = flow.target(skuOrder.getStatus(), orderOperation);
            boolean success = skuOrderDao.updateStatus(skuOrder.getId(), targetStatus);
            if (!success){
                log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, targetStatus});
                throw new JsonResponseException("update.sku.order.failed");
            }
            Map<String, String> extraMap = skuOrder.getExtra();
            extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getQuantity()));

            Response<Boolean> response = orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,extraMap);
            if (!response.isSuccess()){
                log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, extraMap});
                throw new JsonResponseException("update.sku.order.failed");
            }
        }
        //更新总单记录
        if (!flow.operationAllowed(shopOrder.getStatus(), orderOperation)) {
            log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), orderOperation.getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Integer targetStatus = flow.target(shopOrder.getStatus(), orderOperation);
        boolean success = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus);
        if (!success){
            log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus});
            throw new JsonResponseException("update.shop.order.failed");
        }
    }

    @Transactional
    public void updateOrderStatusAndSkuQuantities4Sku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder, OrderOperation cancelOperation, OrderOperation revokeOperation){
        Flow flow = this.pickOrder();
        //更新子单的取消状态
        if (!flow.operationAllowed(skuOrder.getStatus(),cancelOperation)){
            log.error("order (id:{}) current status:{} not allow operation:{}", skuOrder.getId(), skuOrder.getStatus(), cancelOperation.getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Integer targetStatus = flow.target(skuOrder.getStatus(), cancelOperation);
        boolean success = skuOrderDao.updateStatus(skuOrder.getId(), targetStatus);
        if (!success){
            log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, targetStatus});
            throw new JsonResponseException("update.sku.order.failed");
        }
        //更新待处理数量
        Map<String, String> extraMap = skuOrder.getExtra();
        extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getQuantity()));
        Response<Boolean> response =orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,extraMap);
        if (!response.isSuccess()){
            log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, extraMap});
            throw new JsonResponseException("update.sku.order.failed");
        }
        //判断是否存在需要恢复成待处理状态的子单
        if (skuOrders.size()==0)
        {
            //更新总单状态为取消
            if (!flow.operationAllowed(shopOrder.getStatus(), cancelOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), cancelOperation.getText());
                throw new JsonResponseException("order.status.invalid");
            }
            Integer targetStatus1 = flow.target(shopOrder.getStatus(), cancelOperation);
            boolean success2 = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus1);
            if (!success2){
                log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus1});
                throw new JsonResponseException("update.shop.order.failed");
            }
        }else{
            //更新总单的待处理状态
            if (!flow.operationAllowed(shopOrder.getStatus(), revokeOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), revokeOperation.getText());
                throw new JsonResponseException("order.status.invalid");
            }
            Integer targetStatus1 = flow.target(shopOrder.getStatus(), revokeOperation);
            boolean success2 = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus1);
            if (!success2){
                log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus1});
                throw new JsonResponseException("update.shop.order.failed");
            }
            //更新其他子单的待处理状态以及恢复待处理数量
            for (SkuOrder skuOrder1:skuOrders){
                boolean success3 = skuOrderDao.updateStatus(skuOrder1.getId(), MiddleOrderStatus.WAIT_HANDLE.getValue());
                if (!success3){
                    log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{skuOrder1.getStatus(), OrderLevel.SKU, MiddleOrderStatus.WAIT_HANDLE.getValue()});
                    throw new JsonResponseException("update.sku.order.failed");
                }
                Map<String, String> extraMap1 = skuOrder1.getExtra();
                extraMap1.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder1.getQuantity()));
                Response<Boolean> response1 =orderWriteService.updateOrderExtra(skuOrder1.getId(),OrderLevel.SKU,extraMap1);
                if (!response1.isSuccess()){
                    log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder1.getStatus(), OrderLevel.SKU, extraMap1});
                    throw new JsonResponseException("update.sku.order.failed");
                }
            }
        }
    }


    public Flow pickOrder() {
        return MiddleFlowBook.orderFlow;
    }
}
