package com.pousheng.middle.open.service;

import com.google.common.collect.Lists;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.vip.component.VipAfterSaleComponent;
import io.terminus.open.client.vip.component.VipClientFactory;
import io.terminus.open.client.vip.order.VipAfterSaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vipapis.delivery.OrderReturn;
import vipapis.delivery.OrderReturnResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Xiongmin
 * 2019/5/8
 */
@Primary
@Profile("prepub")
@Component
@ConditionalOnProperty(name = "mock.after.sale.common.fetch.enable", havingValue = "true")
public class MockAfterSaleService extends VipAfterSaleService {

    private static final int TOTAL = 5000;

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(VipAfterSaleService.class);
    private VipAfterSaleComponent vipAfterSaleComponent;

    @Autowired
    public MockAfterSaleService(VipClientFactory clientFactory, VipAfterSaleComponent vipAfterSaleComponent) {
        super(clientFactory, vipAfterSaleComponent);
        this.vipAfterSaleComponent = vipAfterSaleComponent;
    }

    @Override
    public Response<Pagination<OpenClientAfterSale>> searchAfterSale(Long openShopId, OpenClientAfterSaleStatus status,
                                                                     Date startAt, Date endAt, Integer pageNo, Integer pageSize) {
        List<OpenClientAfterSale> openClientAfterSaleList = Lists.newArrayList();
        int totalPage = (TOTAL + pageSize - 1) / pageSize ;
        if (pageNo > totalPage) {
            return Response.ok(Pagination.empty());
        }
        OrderReturnResponse orderReturnResponse = mockOrderReturnResponse(pageNo, pageSize);
        orderReturnResponse.getOrder_return_list().forEach((orderReturn) -> {
            OpenClientAfterSale openClientAfterSale = vipAfterSaleComponent.getOrderReturnList(openShopId, orderReturn);
            if (!Objects.isNull(openClientAfterSale)) {
                openClientAfterSaleList.add(openClientAfterSale);
            }

        });
        boolean hasNext = TOTAL > pageNo * pageSize;
        log.info("MOCK VIP-OXO-searchAfterSale,return:{}", openClientAfterSaleList.toString());
        return Response.ok(new Pagination((long)TOTAL, openClientAfterSaleList, hasNext));
    }

    private OrderReturnResponse mockOrderReturnResponse(Integer pageNo, Integer pageSize) {
        Date now = new Date();
        Integer startIndex = (pageNo - 1) * pageSize + 1;
        Integer endIndex = startIndex + pageSize;
        endIndex = endIndex <= TOTAL ? endIndex : TOTAL;
        List<OrderReturn> orderReturns = Lists.newArrayList();
        for (; startIndex <= endIndex; startIndex++) {
            OrderReturn orderReturn = mockOrderReturn(now, startIndex);
            orderReturns.add(orderReturn);
        }
        OrderReturnResponse orderReturnResponse = new OrderReturnResponse();
        orderReturnResponse.setTotal(TOTAL);
        orderReturnResponse.setOrder_return_list(orderReturns);
        return orderReturnResponse;
    }

    private OrderReturn mockOrderReturn(Date now, Integer currentIndex) {
        String orderReturnId = now.getTime() + "" + currentIndex;
        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setCarrier("中国邮政速递物流");
        orderReturn.setOrder_id(now.getTime() + "");
        orderReturn.setOrder_return_id(orderReturnId);
        orderReturn.setReturn_reason("不想要了");
        orderReturn.setReturn_time(SIMPLE_DATE_FORMAT.format(now));
        orderReturn.setReturn_type(1);
        orderReturn.setTransport_no("1000000454");
        orderReturn.setVendor_need_audit(1);
        return orderReturn;
    }
}
