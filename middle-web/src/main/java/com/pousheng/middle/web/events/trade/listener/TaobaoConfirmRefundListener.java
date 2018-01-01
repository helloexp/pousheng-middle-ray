package com.pousheng.middle.web.events.trade.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.AfterSaleServiceRegistryCenter;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.service.OpenClientAfterSaleService;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * 淘宝退货退款单一旦显示退货完成，需要主动拉取淘宝的售后单的状态，将售后退货退款单的状态改为已退款
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/12/29
 * pousheng-middle
 */
@Slf4j
@Component
public class TaobaoConfirmRefundListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;
    @RpcConsumer
    private RefundWriteService refundWriteService;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void updateRefundStatusForTaobao(TaobaoConfirmRefundEvent event){
        OpenClientAfterSaleService afterSaleService = this.afterSaleServiceRegistryCenter.getAfterSaleService(MiddleChannel.TAOBAO.getValue());
        Response<OpenClientAfterSale> r =  afterSaleService.findByAfterSaleId(Long.valueOf(event.getOpenShopId()),event.getOpenAfterSaleId());
        if (!r.isSuccess()){
            log.error("find taobao afterSaleOrder failed,taobaoAfterSaleOrderId is {},refundId is{},caused by{}",event.getOpenAfterSaleId(),event.getRefundId(),event.getOpenShopId());
            return;
        }
        //如果淘宝售后单状态是success(已退款),中台售后单状态同样变成已退款
        OpenClientAfterSale afterSale = r.getResult();
        if (Objects.equals(afterSale.getStatus(), OpenClientAfterSaleStatus.SUCCESS)){
            Response<Boolean> updateR = refundWriteService.updateStatus(event.getRefundId(), MiddleRefundStatus.REFUND.getValue());
            if (!updateR.isSuccess()) {
                log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                        event.getRefundId(), MiddleRefundStatus.REFUND.getValue(), afterSale, updateR.getError());
            }
        }
    }
}
