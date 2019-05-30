package com.pousheng.middle.web.order.component;

import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.web.order.dto.OrderFetchDTO;
import com.pousheng.middle.web.order.dto.OrderFetchTypeConstants;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.center.job.aftersale.component.AfterSaleExchangeExecutor;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * @author Xiongmin
 * 2019/5/5
 */
/*@Primary
@Component*/
@Slf4j
public class MiddleAfterSaleExchangeExecutor extends AfterSaleExchangeExecutor {

    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;
    @Autowired
    private RocketMqProducerService rocketMqProducerService;

    @Value("${open.client.sync.after.sale.fetch.size: 20}")
    private Integer afterSaleFetchSize;

    @Value("${order.fetch.mq.queue.size:0}")
    public Integer orderFetchMqQueueSize;

    @Autowired
    public MiddleAfterSaleExchangeExecutor(@Value("${shop.max.pool.size: 20}") int maxPoolSizeOfShop,
                                           @Value("${after.sale.queue.size: 20000}") int queueSizeOfAfterSale) {
        super(maxPoolSizeOfShop, queueSizeOfAfterSale);
    }

    @Override
    public void syncAfterSaleExchange(OpenShop openShop,
                                      OpenClientAfterSaleStatus afterSaleStatus,
                                      Date startAt,
                                      Date endAt) {
        OpenClientAfterSaleExchangeService afterSaleExchangeService =
                afterSaleExchangeServiceRegistryCenter.getAfterSaleExchangeService(openShop.getChannel());
        Response<Pagination<OpenClientAfterSale>> findResp =
                afterSaleExchangeService.searchAfterSaleExchange(openShop.getId(), afterSaleStatus,
                startAt, endAt, 1, 1);
        if (!findResp.isSuccess()) {
            log.error("fail to find after sale exchange for open shop(id={}) with status={},pageNo={},pageSize={},cause:{}",
                    openShop.getId(), afterSaleStatus, 1, 1, findResp.getError());
            return;
        }
        int total = findResp.getResult().getTotal().intValue();
        int totalPage = (total + afterSaleFetchSize - 1) / afterSaleFetchSize;
        for (int pageNo=1; pageNo <= totalPage; pageNo++) {
            String messageKey = openShop.getId() + ":" + pageNo + "_" + afterSaleFetchSize + "_" + OrderFetchTypeConstants.AFTER_SALE_EXCHANGE;
            OrderFetchDTO orderFetchDTO = new OrderFetchDTO();
            orderFetchDTO.setOpenShopId(openShop.getId());
            orderFetchDTO.setPageNo(pageNo);
            orderFetchDTO.setPageSize(afterSaleFetchSize);
            orderFetchDTO.setStartTime(startAt);
            orderFetchDTO.setEndTime(endAt);
            orderFetchDTO.setOrderFetchType(OrderFetchTypeConstants.AFTER_SALE_EXCHANGE);
            orderFetchDTO.setAfterSaleStatus(afterSaleStatus);
            String message = JsonMapper.nonEmptyMapper().toJson(orderFetchDTO);
            rocketMqProducerService.asyncSendOrderly(MqConstants.POUSHENG_MIDDLE_ORDER_FETCH_TOPIC, message, messageKey, orderFetchMqQueueSize);
        }
    }
}
