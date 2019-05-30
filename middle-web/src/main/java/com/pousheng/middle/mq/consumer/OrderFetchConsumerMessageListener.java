package com.pousheng.middle.mq.consumer;

import com.google.common.base.Throwables;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.web.order.dto.OrderFetchDTO;
import com.pousheng.middle.web.order.dto.OrderFetchTypeConstants;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import io.terminus.common.utils.JsonMapper;
import io.terminus.dingtalk.DingTalkNotifies;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.center.AfterSaleServiceRegistryCenter;
import io.terminus.open.client.center.job.aftersale.api.AfterSaleExchangeReceiver;
import io.terminus.open.client.center.job.aftersale.api.AfterSaleReceiver;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.center.job.order.api.OrderSearchNotifier;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import io.terminus.open.client.order.service.OpenClientAfterSaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import static com.pousheng.middle.web.order.dto.OrderFetchTypeConstants.*;

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
    @Autowired
    private AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;
    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;
    @Autowired
    private AfterSaleReceiver afterSaleReceiver;
    @Autowired
    private AfterSaleExchangeReceiver afterSaleExchangeReceiver;
    @Autowired
    private DingTalkNotifies dingTalkNotifies;

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
        try {
            fetchOrders(orderFetchDTO, openShop);
        } catch (Exception e) {
            String errorMsg = String.format("MQ 拉单异常，orderFetchDTO:%s. 异常详情:%s",
                    orderFetchDTO, Throwables.getStackTraceAsString(e));
            log.error(errorMsg);
            dingTalkNotifies.addMsg(errorMsg);
        }
    }

    private void fetchOrders(OrderFetchDTO orderFetchDTO, OpenShop openShop) {
        switch (orderFetchDTO.getOrderFetchType()) {
            case PAID:
                syncPaid(orderFetchDTO, openShop);
                break;
            case PRE_SALE:
                syncPreSale(orderFetchDTO, openShop);
                break;
            case AFTER_SALE_COMMON:
                syncAfterSaleCommon(orderFetchDTO, openShop);
                break;
            case AFTER_SALE_EXCHANGE:
                syncAfterSaleExchange(orderFetchDTO, openShop);
                break;
            default:
                log.error("orderFetchType:{} mismatch", orderFetchDTO.getOrderFetchType());
                return;
        }
    }

    private void syncPaid(OrderFetchDTO orderFetchDTO, OpenShop openShop) {
        Response<Pagination<OpenClientFullOrder>> findResp = orderServiceCenter.searchOrder(
                orderFetchDTO.getOpenShopId(), orderFetchDTO.getOpenClientOrderStatus(), orderFetchDTO.getStartTime(),
                orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
        if (!findResp.isSuccess()) {
            log.error("fail to fetch paid order for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}, cause:{}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize(), findResp.getError());
            dingTalkNotifies.addMsg(String.format("已付款拉单失败：%s", findResp.getError()));
            return;
        }
        if (CollectionUtils.isEmpty(findResp.getResult().getData())) {
            log.info("SYNC-PAID-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize());
            return;
        }
        log.debug("paid.receiveOrder, openShopId:{}, results:{}",
                openShop.getId(), findResp.getResult().getData());
        orderReceiver.receiveOrder(OpenClientShop.from(openShop), findResp.getResult().getData());
    }

    private void syncPreSale(OrderFetchDTO orderFetchDTO, OpenShop openShop) {
        Response<Pagination<OpenClientFullOrder>> findResp = orderServiceCenter.searchPresale(orderFetchDTO.getOpenShopId(),
                orderFetchDTO.getStartTime(), orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
        if (!findResp.isSuccess()) {
            log.error("fail to fetch pre-sale order for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}, cause:{}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize(), findResp.getError());
            dingTalkNotifies.addMsg(String.format("预售拉单失败：%s", findResp.getError()));
            return;
        }
        if (CollectionUtils.isEmpty(findResp.getResult().getData())) {
            log.info("SYNC-PRE-SALE-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize());
            return;
        }
        log.debug("preSale.receiveOrder, openShopId:{}, results:{}", openShop.getId(), findResp.getResult().getData());
        orderReceiver.receiveOrder(OpenClientShop.from(openShop), findResp.getResult().getData());
    }

    private void syncAfterSaleCommon(OrderFetchDTO orderFetchDTO, OpenShop openShop) {
        OpenClientAfterSaleService afterSaleService =
                afterSaleServiceRegistryCenter.getAfterSaleService(openShop.getChannel());
        Response<Pagination<OpenClientAfterSale>> findResp = afterSaleService.searchAfterSale(openShop.getId(), orderFetchDTO.getAfterSaleStatus(),
                orderFetchDTO.getStartTime(), orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
        if (!findResp.isSuccess()) {
            log.error("fail to fetch after-sale-common order for open shop(id={}) with pageNo={},pageSize={},cause:{}",
                    openShop.getId(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize(), findResp.getError());
            dingTalkNotifies.addMsg(String.format("售后拉单失败：%s", findResp.getError()));
            return;
        }
        if (CollectionUtils.isEmpty(findResp.getResult().getData())) {
            log.info("SYNC-AFTER-SALE-COMMON-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize());
            return;
        }
        log.debug("afterSaleReceiver.receiveAfterSale, openShopId:{}, results:{}",
                openShop.getId(), findResp.getResult().getData());
        afterSaleReceiver.receiveAfterSale(OpenClientShop.from(openShop), findResp.getResult().getData());
    }

    private void syncAfterSaleExchange(OrderFetchDTO orderFetchDTO, OpenShop openShop) {
        OpenClientAfterSaleExchangeService afterSaleExchangeService =
                afterSaleExchangeServiceRegistryCenter.getAfterSaleExchangeService(openShop.getChannel());
        Response<Pagination<OpenClientAfterSale>> findResp = afterSaleExchangeService.searchAfterSaleExchange(openShop.getId(), orderFetchDTO.getAfterSaleStatus(),
                orderFetchDTO.getStartTime(), orderFetchDTO.getEndTime(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize());
        if (!findResp.isSuccess()) {
            log.error("fail to fetch after-sale-exchange order for open shop(id={}) with pageNo={}, pageSize={},cause:{}",
                    openShop.getId(), orderFetchDTO.getPageNo(), orderFetchDTO.getPageSize(), findResp.getError());
            dingTalkNotifies.addMsg(String.format("收货换货拉单失败：%s", findResp.getError()));
            return;
        }
        if (CollectionUtils.isEmpty(findResp.getResult().getData())) {
            log.info("SYNC-AFTER-SALE-EXCHANGE-ORDER-EMPTY for open shop(id={}) with fetchType={}, pageNo={}, pageSize={}",
                    orderFetchDTO.getOpenShopId(), orderFetchDTO.getOrderFetchType(), orderFetchDTO.getPageNo(),
                    orderFetchDTO.getPageSize());
            return;
        }
        log.debug("afterSaleExchangeReceiver.receiveAfterSaleExchange, openShopId:{}, results:{}",
                openShop.getId(), findResp.getResult().getData());
        afterSaleExchangeReceiver.receiveAfterSaleExchange(OpenClientShop.from(openShop), findResp.getResult().getData());
    }
}