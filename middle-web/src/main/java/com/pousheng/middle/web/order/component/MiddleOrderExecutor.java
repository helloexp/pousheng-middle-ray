package com.pousheng.middle.web.order.component;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.web.order.dto.OrderFetchDTO;
import com.pousheng.middle.web.order.dto.OrderFetchTypeConstants;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.OrderServiceRegistryCenter;
import io.terminus.open.client.center.job.order.component.OrderExecutor;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.open.client.order.service.OpenClientOrderService;
import io.terminus.open.client.vip.order.VipOrderService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Xiongmin
 * 2019/4/28
 */
@Primary
@Component
@Slf4j
public class MiddleOrderExecutor extends OrderExecutor {

    @Autowired
    private OrderServiceCenter orderServiceCenter;
    @Autowired
    private RocketMqProducerService rocketMqProducerService;
    @Autowired
    private OrderServiceRegistryCenter orderServiceRegistryCenter;
    @Autowired
    private OpenShopCacher openShopCacher;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${open.client.sync.order.fetch.size: 20}")
    private Integer orderFetchSize;
    @Value("${order.fetch.mq.queue.size:0}")
    public Integer orderFetchMqQueueSize;

    @Autowired
    public MiddleOrderExecutor(@Value("${shop.max.pool.size: 20}") int maxPoolSizeOfShop,
                               @Value("${order.queue.size: 500000}") int queueSizeOfOrder) {
        super(maxPoolSizeOfShop, queueSizeOfOrder);
    }

    @Override
    public void syncOrder(OpenShop openShop,
                          OpenClientOrderStatus orderStatus,
                          Date startAt,
                          Date endAt) {
        OpenClientOrderService openClientOrderService = getService(openShop.getId());
        // 唯品会的没有总页码，因此走
        if (openClientOrderService instanceof VipOrderService) {
            super.syncOrder(openShop, orderStatus, startAt, endAt);
            return;
        }
        if (openShop.getShopName().startsWith("yj")){
            return;
        }
        log.info("SYNC-PAID-ORDER-MQ-START for shop id:{} shop name:{} startAt:{} endAt:{} begin:{}",openShop.getId(),
                openShop.getShopName(),
                DFT.print(new DateTime(startAt)),
                DFT.print(new DateTime(endAt)),
                DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        Response<Pagination<OpenClientFullOrder>> findResp = orderServiceCenter.searchOrder(openShop.getId(), orderStatus,
                startAt, endAt, 1, 1);
        if (!findResp.isSuccess()) {
            log.error("fail to find order for open shop(id={}) with status={},pageNo={},pageSize={},cause:{}",
                    openShop.getId(), orderStatus, 1, 1, findResp.getError());
            return;
        }
        int total = findResp.getResult().getTotal().intValue();
        int totalPage = (total + orderFetchSize - 1) / orderFetchSize;
        for (int pageNo=1; pageNo <= totalPage; pageNo++) {
            String messageKey = openShop.getId() + ":" + pageNo + "_" + orderFetchSize + "_" + OrderFetchTypeConstants.PAID;
            OrderFetchDTO orderFetchDTO = new OrderFetchDTO();
            orderFetchDTO.setOpenShopId(openShop.getId());
            orderFetchDTO.setPageNo(pageNo);
            orderFetchDTO.setPageSize(orderFetchSize);
            orderFetchDTO.setStartTime(startAt);
            orderFetchDTO.setEndTime(endAt);
            orderFetchDTO.setOrderFetchType(OrderFetchTypeConstants.PAID);
            orderFetchDTO.setOpenClientOrderStatus(orderStatus);
            String message = JsonMapper.nonEmptyMapper().toJson(orderFetchDTO);
            rocketMqProducerService.asyncSendOrderly(MqConstants.POUSHENG_MIDDLE_ORDER_FETCH_TOPIC, message, messageKey, orderFetchMqQueueSize);
        }
        stopwatch.stop();
        log.info("SYNC-PAID-ORDER-MQ-END for shop id:{} shop name:{} startAt:{} endAt:{} done at {} cost {} ms",openShop.getId(),
                openShop.getShopName(),
                DFT.print(new DateTime(startAt)),
                DFT.print(new DateTime(endAt)),
                DFT.print(DateTime.now()),
                stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * 批量处理为支付的单据
     * @param openShop
     * @param startAt
     * @param endAt
     */
    @Override
    public void syncNotPaidOrder(OpenShop openShop, Date startAt, Date endAt) {
        log.info("SYNC-NOT-PAID-ORDER-MQ-START for shop id:{} shop name:{} startAt:{} endAt:{} begin:{}",openShop.getId(),
                openShop.getShopName(),
                DFT.print(new DateTime(startAt)),
                DFT.print(new DateTime(endAt)),
                DFT.print(DateTime.now()));
        Response<Pagination<OpenClientFullOrder>> findResp = orderServiceCenter.searchPresale(openShop.getId(),
                startAt, endAt, 1, 1);
        if (!findResp.isSuccess()) {
            log.error("fail to find not paid order for open shop(id={}) with pageNo={},pageSize={},cause:{}",
                    openShop.getId(), 1, 1, findResp.getError());
            return;
        }
        int total = findResp.getResult().getTotal().intValue();
        int totalPage = (total + orderFetchSize - 1) / orderFetchSize;
        for (int pageNo=1; pageNo <= totalPage; pageNo++) {
            String messageKey = openShop.getId() + ":" + pageNo + "_" + orderFetchSize + "_" + OrderFetchTypeConstants.PRE_SALE;
            OrderFetchDTO orderFetchDTO = new OrderFetchDTO();
            orderFetchDTO.setOpenShopId(openShop.getId());
            orderFetchDTO.setPageNo(pageNo);
            orderFetchDTO.setPageSize(orderFetchSize);
            orderFetchDTO.setStartTime(startAt);
            orderFetchDTO.setEndTime(endAt);
            orderFetchDTO.setOrderFetchType(OrderFetchTypeConstants.PRE_SALE);
            String message = JsonMapper.nonEmptyMapper().toJson(orderFetchDTO);
            rocketMqProducerService.asyncSendOrderly(MqConstants.POUSHENG_MIDDLE_ORDER_FETCH_TOPIC, message, messageKey, orderFetchMqQueueSize);
        }
    }

    private OpenClientOrderService getService(Long shopId) {
        return orderServiceRegistryCenter.getOrderService(openShopCacher.findById(shopId).getChannel());
    }
}
