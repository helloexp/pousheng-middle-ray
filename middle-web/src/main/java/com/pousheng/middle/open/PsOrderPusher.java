package com.pousheng.middle.open;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.OpenPushOrderTask;
import com.pousheng.middle.order.service.OpenPushOrderTaskWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.component.DefaultOrderPusher;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private OpenPushOrderTaskWriteService openPushOrderTaskWriteService;

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
                //补偿任务
                for (OpenFullOrderInfo openFullOrderInfo:openFullOrderInfos){
                    try{
                        OpenPushOrderTask openPushOrderTask =  new OpenPushOrderTask();
                        openPushOrderTask.setChannel(openFullOrderInfo.getOrder().getChannel());
                        openPushOrderTask.setSourceOrderId(openFullOrderInfo.getOrder().getOutId());
                        openPushOrderTask.setStatus(0);
                        Map<String, String> extra = Maps.newHashMap();
                        String openClientShopJson = JsonMapper.nonEmptyMapper().toJson(openClientShop);
                        String openFullOrderInfosJson = JsonMapper.nonEmptyMapper().toJson(Lists.newArrayList(openFullOrderInfo));
                        extra.put("openClientShop",openClientShopJson);
                        extra.put("orderInfos",openFullOrderInfosJson);
                        openPushOrderTask.setExtra(extra);
                        Response<Long> response = openPushOrderTaskWriteService.create(openPushOrderTask);
                        if (!response.isSuccess()){
                            log.error("create open push order task failed,openShopId is {},orders are {},caused by {}",openClientShop.getOpenShopId(),openFullOrderInfo,r.getError());
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

        }catch (Exception e){
            log.error("sync order to out failed,openShopId is {},orders are {},caused by {}",openClientShop.getOpenShopId(),openFullOrderInfos,e.getMessage());
        }
    }
}
