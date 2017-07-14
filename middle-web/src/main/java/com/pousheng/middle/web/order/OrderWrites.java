package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by songrenfei on 2017/7/6
 */
@RestController
@Slf4j
public class OrderWrites {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;

    /**
     * 取消订单
     * @param shopOrderId 订单id
     */
    @RequestMapping(value = "api/order/{id}/cancel",method = RequestMethod.PUT)
    public void cancelOrder(@PathVariable(value = "id") Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        orderWriteLogic.updateOrder(shopOrder, OrderLevel.SHOP,MiddleOrderEvent.CANCEL);
    }

}