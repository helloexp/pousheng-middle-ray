package com.pousheng.middle.open;

import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderPusher;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
                OpenFullOrderAddress address = openFullOrderInfo.getAddress();
                receiverInfoCompleter.completePushOrderAddress(address);
                openFullOrderInfo.setAddress(address);
            }
            //如果手机号，会员卡号为空，以及买家信息为空该如何处理
            for (OpenFullOrderInfo openFullOrderInfo:openFullOrderInfos){
                OpenFullOrder order = openFullOrderInfo.getOrder();
                OpenFullOrderAddress address = openFullOrderInfo.getAddress();
                //如果买家用户名为空，则使用地址中的买家收货信息
                if (!StringUtils.hasText(order.getBuyerName())){
                    order.setBuyerName(address.getReceiveUserName());
                }
                //如果买家手机号为空，则使用地址中的手机号
                if (!StringUtils.hasText(order.getBuyerMobile())){
                    order.setBuyerMobile(address.getPhone());
                }
                //如果会员卡号为空，则默认传入0
                if (!StringUtils.hasText(order.getMemberCardId())){
                    order.setMemberCardId("0");
                }
                openFullOrderInfo.setOrder(order);
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
