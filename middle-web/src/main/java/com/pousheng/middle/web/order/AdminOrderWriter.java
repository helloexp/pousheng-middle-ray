package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
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
    public void syncOrderInfoToEcp(@PathVariable(value = "id") Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Response<Boolean> syncRes =syncOrderToEcpLogic.syncOrderToECP(shopOrder);
        if(!syncRes.isSuccess()){
            log.error("sync shopOrder(id:{}) to ecp fail,error:{}",shopOrderId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

    }

    /**
     * 删除子订单
     * @param id shopOrder的id
     * @param skuCode
     */
    @RequestMapping(value = "api/order/{id}/cancel/sku/order", method = RequestMethod.PUT)
    public void cancelSkuOrder(@PathVariable("id") Long id, @RequestParam("skuCode") String skuCode) {
        orderWriteLogic.cancelSkuOrder(id, skuCode);
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
