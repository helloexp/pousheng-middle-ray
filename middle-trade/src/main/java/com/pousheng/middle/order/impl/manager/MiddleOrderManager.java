package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.impl.PsShopOrderStatusStrategy;
import com.pousheng.middle.order.impl.dao.ShopOrderExtDao;
import com.pousheng.middle.order.model.ShopOrderExt;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.OrderWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
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
    @Autowired
    private ShopOrderExtDao shopOrderExtDao;
    @Autowired
    private PsShopOrderStatusStrategy psOrderStatusStrategy;

    private static final JsonMapper mapper=JsonMapper.JSON_NON_EMPTY_MAPPER;

    /**
     * 更新总单与子单的状态,适用于总单的取消和撤销流程
     *
     * @param shopOrder 总单
     * @param skuOrders 子单集合
     * @param orderOperation 传入的操作动作,用于下一步操作
     */
    @Transactional
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
            extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getWithHold()));
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
     * 更新总单与子单的状态
     * @param shopOrder 总单
     * @param orderOperation 传入的操作动作,用于下一步操作
     */
    @Transactional
    public void updateOrderStatusForJit(ShopOrder shopOrder, OrderOperation orderOperation) {
        Flow flow = this.pickOrder();
        //更新总单记录
        if (!flow.operationAllowed(shopOrder.getStatus(), orderOperation)) {
            log.error("order (id:{}) current status:{} not allow operation:{}", shopOrder.getId(), shopOrder.getStatus(), orderOperation.getText());
            throw new ServiceException("order.status.invalid");
        }

        Integer targetStatus = flow.target(shopOrder.getStatus(), orderOperation);

        skuOrderDao.updateStatusByOrderId(shopOrder.getId(),shopOrder.getStatus(),targetStatus);

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
     * @param cancelList 需要取消的子单
     * @param cancelOperation 取消的动作
     * @param revokeOperation 附加动作,可用于总单,和其他恢复成待处理的子单的操作
     * @param skuCode
     */
    @Transactional(rollbackFor = ServiceException.class)
    public void updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, List<SkuOrder> cancelList, OrderOperation cancelOperation, OrderOperation revokeOperation,String skuCode){
        Flow flow = this.pickOrder();
        //更新子单的取消状态
        if (cancelList.size() > 0) {
            for (SkuOrder skuOrder : cancelList) {
                if (!flow.operationAllowed(skuOrder.getStatus(), cancelOperation)) {
                    log.error("order (id:{}) current status:{} not allow operation:{}", skuOrder.getId(), skuOrder.getStatus(), cancelOperation.getText());
                    throw new ServiceException("order.status.invalid");
                }
                Integer targetStatus = flow.target(skuOrder.getStatus(), cancelOperation);
                Map<String, String> extraMap = skuOrder.getExtra();
                extraMap.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder.getWithHold()));
                SkuOrder newSkuOrder = new SkuOrder();
                newSkuOrder.setId(skuOrder.getId());
                newSkuOrder.setExtra(extraMap);
                newSkuOrder.setStatus(targetStatus);
                boolean res1 = this.skuOrderDao.update(newSkuOrder);
                if (!res1) {
                    log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder.getStatus(), OrderLevel.SKU, extraMap});
                    throw new ServiceException("update.sku.order.failed");
                }
            }
        }
        if (skuOrders.size() > 0) {
            //更新其他子单的待处理状态以及恢复待处理数量
            for (SkuOrder skuOrder1 : skuOrders) {
                skuOrder1 = skuOrderDao.findById(skuOrder1.getId());
                if (!flow.operationAllowed(skuOrder1.getStatus(), revokeOperation)) {
                    log.info("this sku order current status is {}, can not revoke", skuOrder1.getStatus());
                    continue;
                }
                Map<String, String> extraMap1 = skuOrder1.getExtra();
                extraMap1.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(skuOrder1.getQuantity()));
                SkuOrder newSkuOrder1 = new SkuOrder();
                newSkuOrder1.setId(skuOrder1.getId());
                newSkuOrder1.setExtra(extraMap1);
                newSkuOrder1.setStatus(MiddleOrderStatus.WAIT_HANDLE.getValue());
                boolean res3 = this.skuOrderDao.update(newSkuOrder1);
                if (!res3) {
                    log.error("failed to update order(id={}, level={})'s extraMap to : {}", new Object[]{skuOrder1.getStatus(), OrderLevel.SKU, extraMap1});
                    throw new ServiceException("update.sku.order.failed");
                }
            }
        }

        List<SkuOrder> skuOrderList = skuOrderDao.findByOrderId(shopOrder.getId());
        Integer targetStatus1 = this.psOrderStatusStrategy.status(skuOrderList);
        boolean success2 = shopOrderDao.updateStatus(shopOrder.getId(), targetStatus1);
        if (!success2){
            log.error("failed to update order(id={}, level={})'s status to : {}", new Object[]{shopOrder.getStatus(), OrderLevel.SHOP, targetStatus1});
            throw new ServiceException("update.shop.order.failed");
        }




    }

    /**
     *更新订单收货信息以及买家备注
     * @param shopOrderId 店铺订单主键
     * @param orderReceiverInfo 收货信息
     * @param buyerNote 买家备注
     *
     */
    @Transactional
    public void updateReceiverInfoAndBuyerNote(long shopOrderId,OrderReceiverInfo orderReceiverInfo,String buyerNote){
        boolean recevierInfoResult = orderReceiverInfoDao.update(orderReceiverInfo);
        if (!recevierInfoResult){
            log.error("failed to update orderReceiveInfo failed,(shopOrderId={}))",shopOrderId);
            throw new ServiceException("receiveInfo.update.fail");
        }
        ShopOrderExt shopOrderExt = new ShopOrderExt();
        shopOrderExt.setId(shopOrderId);
        shopOrderExt.setBuyerNote(buyerNote);
        if(null != orderReceiverInfo.getReceiverInfo()){
            shopOrderExt.setOutBuyerId(orderReceiverInfo.getReceiverInfo().getMobile());
        }
        //更新订单表中的手机号字段（中台是使用outBuyerId作为手机号）
        boolean shopOrderResult = shopOrderExtDao.update(shopOrderExt);
        if (!shopOrderResult){
            log.error("failed to update shopOrder failed,(shopOrderId={})),buyerNote(={})",shopOrderId,buyerNote);
            throw new ServiceException("receiveInfo.update.fail");
        }
    }

    public Flow pickOrder() {
        return MiddleFlowBook.orderFlow;
    }

    /**
     * 批量更新订单状态 适用于JIT时效订单
     * @param shopOrderIds
     * @param skuOrderIds
     */
    @Transactional
    public void updateOrderStatus(List<Long> shopOrderIds,List<Long> skuOrderIds,Integer status){
        String exceptionMsg;
        boolean flag=false;
        for(Long orderId:shopOrderIds){
            flag=shopOrderDao.updateStatus(orderId,status);
            if(!flag){
                exceptionMsg= MessageFormat.format("failed to update shop order[{0}] to status[{1}].param:{2}",
                    orderId,status,mapper.toJson(shopOrderIds));
                log.error(exceptionMsg);
                throw new ServiceException(exceptionMsg);
            }
        }

        for(Long skuOrderId:skuOrderIds){
            flag=skuOrderDao.updateStatus(skuOrderId,status);
            if(!flag){
                exceptionMsg= MessageFormat.format("failed to update sku order[{0}] to status[{1}].param:{2}",
                    skuOrderId,status,mapper.toJson(skuOrderIds));
                log.error(exceptionMsg);
                throw new ServiceException(exceptionMsg);
            }
        }
    }

}
