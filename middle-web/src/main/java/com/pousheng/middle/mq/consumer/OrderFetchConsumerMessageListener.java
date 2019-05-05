package com.pousheng.middle.mq.consumer;

import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.web.order.dto.OrderFetchDTO;
import com.pousheng.middle.web.order.dto.OrderFetchTypeConstants;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.center.job.order.api.OrderSearchNotifier;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;
import static com.pousheng.middle.web.order.dto.OrderFetchTypeConstants.PAID;
import static com.pousheng.middle.web.order.dto.OrderFetchTypeConstants.PRE_SALE;

/**
 * @author Xiongmin
 * 2019/4/28
 */
@ConditionalOnProperty(name = "order.fetch.topic.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@Service
@MQConsumer
public class OrderFetchConsumerMessageListener {

    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private OrderSearchNotifier orderSearchNotifier;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private OrderReceiver orderReceiver;

    @MQSubscribe(topic = MqConstants.POUSHENG_MIDDLE_ORDER_FETCH_TOPIC,
            consumerGroup =  MqConstants.POUSHENG_MIDDLE_ORDER_FETCH_CONSUMER_GROUP,
            consumeMode = ConsumeMode.CONCURRENTLY)
    public void onMessage(String message) {
        log.debug("OrderFetchConsumerMessageListener onMessage, message {}",message);
        OrderFetchDTO orderFetchDTO = JsonMapper.nonEmptyMapper().fromJson(message, OrderFetchDTO.class);
        OpenShop openShop = openShopCacher.findById(orderFetchDTO.getOpenShopId());
        if (openShop.getShopName().startsWith("yj") &&
                OrderFetchTypeConstants.PAID.equals(orderFetchDTO.getOrderFetchType())){
            return;
        }
        if (orderSearchNotifier != null) {
            orderSearchNotifier.notify(openShop);
        }
        List<OpenClientFullOrder> openClientFullOrders = fetchOrders(orderFetchDTO);
        if (CollectionUtils.isEmpty(openClientFullOrders)) {
            log.info("SYNC-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize());
            return;
        }
        List<String> outerIds = openClientFullOrders.stream().map(OpenClientFullOrder::getOrderId).collect(Collectors.toList());
        log.info("SYNC-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}, outOrderIds:{}",
                orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(),
                orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize(), outerIds);
        orderReceiver.receiveOrder(OpenClientShop.from(openShop), openClientFullOrders);
    }

    private List<OpenClientFullOrder> fetchOrders(OrderFetchDTO orderFetchDTO) {
        Response<Pagination<OpenClientFullOrder>> findResp;
        switch (orderFetchDTO.getOrderFetchType()) {
            case PAID:
                findResp = orderServiceCenter.searchOrder(
                        orderFetchDTO.getOpenShopId(), OpenClientOrderStatus.PAID, orderFetchDTO.getStartTime(),
                        orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
                break;
            case PRE_SALE:
                findResp = orderServiceCenter.searchPresale(orderFetchDTO.getOpenShopId(),
                        orderFetchDTO.getStartTime(), orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
                break;
            default:
                log.error("orderFetchType:{} mismatch", orderFetchDTO.getOrderFetchType());
                findResp = Response.ok();
        }
        if (!findResp.isSuccess()) {
            log.error("fail to fetch order for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}, cause:{}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize(), findResp.getError());
        }
        return findResp.getResult().getData();
    }
}