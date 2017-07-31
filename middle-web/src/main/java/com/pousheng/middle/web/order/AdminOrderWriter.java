package com.pousheng.middle.web.order;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by tony on 2017/7/18.
 * pousheng-middle
 */
@RestController
@Slf4j
public class AdminOrderWriter {
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    /**
     * 发货单已发货,同步订单信息到电商
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/sync/ecp",method = RequestMethod.PUT)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    public void syncOrderInfoToEcp(@PathVariable(value = "id") @PermissionCheckParam Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Response<Boolean> syncRes =syncOrderToEcpLogic.syncOrderToECP(shopOrder);
        if(!syncRes.isSuccess()){
            log.error("sync shopOrder(id:{}) to ecp fail,error:{}",shopOrderId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

    }

    /**
     * 取消子订单(自动)
     * @param shopOrderId
     * @param skuCode
     */
    @RequestMapping(value = "api/order/{id}/auto/cancel/sku/order", method = RequestMethod.PUT)
    public void autoCancelSkuOrder(@PathVariable("id") Long shopOrderId, @RequestParam("skuCode") String skuCode) {
        orderWriteLogic.autoCancelSkuOrder(shopOrderId,skuCode);
    }

    /**
     * 整单撤销,状态恢复成初始状态
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/rollback/shop/order",method = RequestMethod.PUT)
    public void rollbackShopOrder(@PathVariable("id")Long shopOrderId){
      orderWriteLogic.rollbackShopOrder(shopOrderId);
    }

    /**
     * 订单(包括整单和子单)取消失败,手工操作逻辑
     * @param shopOrderId
     */
    @RequestMapping(value="api/order/{id}/cancel/order",method = RequestMethod.PUT)
    public void cancelShopOrder(@PathVariable("id") Long shopOrderId){
        //判断是整单取消还是子单取消
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取是否存在失败的sku记录
        String skuCodeCanceled = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.SKU_CODE_CANCELED,shopOrder);
        if(StringUtils.isNotEmpty(skuCodeCanceled)){
            orderWriteLogic.cancelSkuOrder(shopOrderId,skuCodeCanceled);
        }else{
            orderWriteLogic.cancelShopOrder(shopOrderId);
        }

    }
    /**
     * 整单取消,子单整单发货单状态变为已取消(自动)
     * @param shopOrderId
     */
    @RequestMapping(value="api/order/{id}/auto/cancel/shop/order",method = RequestMethod.PUT)
    public void autoCancelShopOrder(@PathVariable("id") Long shopOrderId){
        orderWriteLogic.autoCancelShopOrder(shopOrderId);
    }



    /**
     * 电商确认收货,此时通知中台修改shopOrder中ecpOrderStatus状态为已完成
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/confirm",method = RequestMethod.PUT)
    public void confirmOrders(@PathVariable("id") Long shopOrderId){
        ShopOrder shopOrder =  orderReadLogic.findShopOrderById(shopOrderId);
        orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.CONFIRM.toOrderOperation());
    }
}
