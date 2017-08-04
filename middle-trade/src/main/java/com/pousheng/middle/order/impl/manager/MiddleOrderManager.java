package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderReceiverInfo;
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
    @Autowired
    private OrderReceiverInfoDao orderReceiverInfoDao;

    /**
     * 更新总单与子单的状态,适用于总单的取消和撤销流程
     *
     * @param shopOrder 总单
     * @param skuOrders 子单集合
     * @param orderOperation 传入的操作动作,用于下一步操作
     */

    public void updateOrderStatusAndSkuQuantities(ShopOrder shopOrder, List<SkuOrder> skuOrders, OrderOperation orderOperation) {
        Flow flow = this.pickOrder();
        //更新sku订单记录
        for (SkuOrder skuOrder : skuOrders) {
            if (!flow.operationAllowed(skuOrder.getStatus(), orderOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", skuOrder.getId(), skuOrder.getStatus(), orderOperation.getText());
                throw new ServiceException("order.status.invalid");
            }
            Integer targetStatus = flow.target(skuOrder.getStatus(), orderOperation);

            Map<String, String> extraMap = skuOrder.getExtra();
            extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getQuantity()));
            SkuOrder newSkuOrder = new SkuOrder();
            newSkuOrder.setId(skuOrder.getId());
            newSkuOrder.setExtra(extraMap);
            newSkuOrder.setStatus(targetStatus);
            boolean res1 = this.skuOrderDao.update(newSkuOrder);
            if (!res1){
                log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, extraMap});
                throw new ServiceException("update.sku.order.failed");
            }
        }
        //更新总单记录
        if (!flow.operationAllowed(shopOrder.getStatus(), orderOperation)) {
            log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), orderOperation.getText());
            throw new ServiceException("order.status.invalid");
        }
        Integer targetStatus = flow.target(shopOrder.getStatus(), orderOperation);
        boolean success = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus);
        if (!success){
            log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus});
            throw new ServiceException("update.shop.order.failed");
        }
    }

    /**
     * 子单取消流程,适用于子单的取消
     *
     * @param shopOrder 总单
     * @param skuOrders 去除了当前取消记录的总单下的其他子单的集合
     * @param skuOrder 需要取消的子单
     * @param cancelOperation 取消的动作
     * @param revokeOperation 附加动作,可用于总单,和其他恢复成待处理的子单的操作
     * @param skuCode
     */
    @Transactional
    public void updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder, OrderOperation cancelOperation, OrderOperation revokeOperation,String skuCode){
        Flow flow = this.pickOrder();
        //更新子单的取消状态
        if (!flow.operationAllowed(skuOrder.getStatus(),cancelOperation)){
            log.error("order (id:{}) current status:{} not allow operation:{}", skuOrder.getId(), skuOrder.getStatus(), cancelOperation.getText());
            throw new ServiceException("order.status.invalid");
        }
        Integer targetStatus = flow.target(skuOrder.getStatus(), cancelOperation);

        Map<String, String> extraMap = skuOrder.getExtra();
        extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getQuantity()));
        SkuOrder newSkuOrder = new SkuOrder();
        newSkuOrder.setId(skuOrder.getId());
        newSkuOrder.setExtra(extraMap);
        newSkuOrder.setStatus(targetStatus);
        boolean res1 = this.skuOrderDao.update(newSkuOrder);
        if (!res1){
            log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, extraMap});
            throw new ServiceException("update.sku.order.failed");
        }
        //订单添加失败的skuCode
        Map<String, String> extra = shopOrder.getExtra();
        extra.put(TradeConstants.SKU_CODE_CANCELED, skuCode);
        boolean res2 = this.shopOrderDao.updateExtra(shopOrder.getId(),extra);
        if (!res2){
            log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SHOP, extra});
            throw new ServiceException("update.sku.order.failed");
        }

        //判断是否存在需要恢复成待处理状态的子单
        if (skuOrders.size()==0)
        {
            //更新总单状态
            if (!flow.operationAllowed(shopOrder.getStatus(), cancelOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), cancelOperation.getText());
                throw new JsonResponseException("order.status.invalid");
            }
            Integer targetStatus1 = flow.target(shopOrder.getStatus(), cancelOperation);
            boolean success2 = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus1);
            if (!success2){
                log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus1});
                throw new ServiceException("update.shop.order.failed");
            }
        }else{
            //更新总单状态
            if (!flow.operationAllowed(shopOrder.getStatus(), revokeOperation)) {
                log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), revokeOperation.getText());
                throw new ServiceException("order.status.invalid");
            }
            Integer targetStatus1 = flow.target(shopOrder.getStatus(), revokeOperation);
            boolean success2 = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus1);
            if (!success2){
                log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus1});
                throw new ServiceException("update.shop.order.failed");
            }
            //更新其他子单的待处理状态以及恢复待处理数量
            for (SkuOrder skuOrder1:skuOrders){
                Map<String, String> extraMap1 = skuOrder1.getExtra();
                extraMap1.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder1.getQuantity()));
                SkuOrder newSkuOrder1 = new SkuOrder();
                newSkuOrder1.setId(skuOrder1.getId());
                newSkuOrder1.setExtra(extraMap1);
                newSkuOrder1.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
                boolean res3 =this.skuOrderDao.update(newSkuOrder1);
                if (!res3){
                    log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder1.getStatus(), OrderLevel.SKU, extraMap1});
                    throw new ServiceException("update.sku.order.failed");
                }
            }
        }
    }

    /**
     *更新订单收货信息以及买家备注
     * @param shopOrderId 店铺订单主键
     * @param orderReceiverInfo 收货信息
     * @param buyerNote 买家备注
     */
    @Transactional
    public void updateReceiverInfoAndBuyerNote(long shopOrderId,OrderReceiverInfo orderReceiverInfo,String buyerNote){
        boolean recevierInfoResult = orderReceiverInfoDao.update(orderReceiverInfo);
        if (!recevierInfoResult){
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={}))",shopOrderId);
            throw new ServiceException("receiveInfo.update.fail");
        }
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(shopOrderId);
        shopOrder.setBuyerNote(buyerNote);
        boolean shopOrderResult = shopOrderDao.update(shopOrder);
        if (!shopOrderResult){
            log.error("failed to update shopOrder failed,(shopOrderId={})),buyerNote(={})",shopOrderId,buyerNote);
            throw new ServiceException("receiveInfo.update.fail");
        }
    }
    public Flow pickOrder() {
        return MiddleFlowBook.orderFlow;
    }
}
