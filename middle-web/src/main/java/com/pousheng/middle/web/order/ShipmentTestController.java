package com.pousheng.middle.web.order;

import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;

@RestController
@Slf4j
public class ShipmentTestController {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;


    @Autowired
    private OrderReadLogic orderReadLogic;
    /**
     *
     * 并发派单测试
     * @param ids
     */
    @RequestMapping(value = "api/shipment/ct", method = RequestMethod.GET)
    public void concurrentShipmentTest(String ids) {
        String[] orderIds=ids.split("-");
        CountDownLatch countDownLatch=new CountDownLatch(orderIds.length);
        for(String id:orderIds) {
            new Thread(() -> {
                log.info("current thread:{},order:{}",Thread.currentThread().getId(),id);
                ShopOrder shopOrder=orderReadLogic.findShopOrderById(Long.valueOf(id));
                countDownLatch.countDown();
                shipmentWiteLogic.doAutoCreateShipment(shopOrder);
            }).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("concurrent error,",e);
        }

    }
}
