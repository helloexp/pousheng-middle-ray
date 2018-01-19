package com.pousheng.middle.open;

import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderPusher;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/19
 * pousheng-middle
 * @author tony
 */
@Component
@Slf4j
public class PsOrderPusher extends DefaultOrderPusher {
    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private ReceiverInfoCompleter receiverInfoCompleter;

    @Override
    public void pushOrders(OpenClientShop openClientShop, List<OpenFullOrderInfo> openFullOrderInfos) {
        try{
            /**
             * 添加地址id
             */
            for (OpenFullOrderInfo openFullOrderInfo:openFullOrderInfos){
                OpenFullOrderAddress address = openFullOrderInfo.getAdderss();
                receiverInfoCompleter.completePushOrderAddress(address);
                openFullOrderInfo.setAdderss(address);
            }

            Response<Boolean> r =  orderServiceCenter.syncOrderToEcp(openClientShop.getOpenShopId(),openFullOrderInfos);
            if (!r.isSuccess()){
                log.error("sync order to out failed,openShopId is {},orders are {},caused by {}",openClientShop.getOpenShopId(),openFullOrderInfos,r.getError());
            }

        }catch (Exception e){
            log.error("sync order to out failed,openShopId is {},orders are {},caused by {}",openClientShop.getOpenShopId(),openFullOrderInfos,e.getMessage());
        }
    }
}
