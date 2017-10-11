package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.events.trade.ModifyMobileEvent;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.model.OrderReceiverInfo;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.service.OrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/10
 * pousheng-middle
 */
@Component
@Slf4j
public class ModifyMobileListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private OrderReadService orderReadService;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;
    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void modifyMobile(ModifyMobileEvent event){
        Long shopOrderId = event.getShopOrderId();
        Response<OrderDetail> rltRes =  orderReadService.findOrderDetailById(shopOrderId);
        if (!rltRes.isSuccess()){
            log.error("find shop order failed,shopOrder id is ({}),caused by {}",shopOrderId,rltRes.getError());
        }else{
            OrderDetail orderDetail = rltRes.getResult();
            List<OrderReceiverInfo> orderReceiverInfos = orderDetail.getOrderReceiverInfos();
            OrderReceiverInfo orderReceiverInfo = orderReceiverInfos.get(0);
            ReceiverInfo receiverInfo = orderReceiverInfo.getReceiverInfo();
            //获取手机号
            String mobile = receiverInfo.getMobile();
            //更新订单表中的手机号字段（中台是使用outBuyerId作为手机号）
            Response<Boolean> response = middleOrderWriteService.updateMobileByShopOrderId(shopOrderId,mobile);
            if (!response.isSuccess()){
                log.error("update mobile failed,shopOrderId is ({}),caused by",shopOrderId,response.getError());
            }
        }
    }
}
