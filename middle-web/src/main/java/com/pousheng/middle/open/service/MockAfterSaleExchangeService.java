package com.pousheng.middle.open.service;

import com.google.common.collect.Lists;
import com.taobao.api.response.TmallExchangeReceiveGetResponse;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.taobao.component.TaobaoAfterSaleExchangeConverter;
import io.terminus.open.client.taobao.component.TaobaoClientFactory;
import io.terminus.open.client.taobao.order.TaobaoAfterSaleExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

/**
 * @author Xiongmin
 * 2019/5/8
 */
@Primary
@Profile("prepub")
@Component
@ConditionalOnProperty(name = "mock.after.sale.exchange.fetch.enable", havingValue = "true")
public class MockAfterSaleExchangeService extends TaobaoAfterSaleExchangeService {

    private static final Long TOTAL = 5000L;

    @Autowired
    private TaobaoAfterSaleExchangeConverter afterSaleExchangeConverter;

    @Autowired
    public MockAfterSaleExchangeService(TaobaoClientFactory clientFactory, TaobaoAfterSaleExchangeConverter afterSaleExchangeConverter) {
        super(clientFactory, afterSaleExchangeConverter);
    }

    @Override
    public Response<Pagination<OpenClientAfterSale>> searchAfterSaleExchange(
        Long openShopId, OpenClientAfterSaleStatus status, Date startAt, Date endAt, Integer pageNo, Integer pageSize) {
        int totalPage = (TOTAL.intValue() + pageSize - 1) / pageSize ;
        if (pageNo > totalPage) {
            return Response.ok(Pagination.empty());
        }
        TmallExchangeReceiveGetResponse mockResp = mockTmallExchangeReceiveGetResponse(pageNo, pageSize);
        return Response.ok(this.afterSaleExchangeConverter.from(mockResp));
    }

    private TmallExchangeReceiveGetResponse mockTmallExchangeReceiveGetResponse(Integer pageNo, Integer pageSize) {
        Date now = new Date();
        Integer startIndex = (pageNo - 1) * pageSize + 1;
        Integer endIndex = startIndex + pageSize;
        endIndex = endIndex <= TOTAL.intValue() ? endIndex : TOTAL.intValue();
        List<TmallExchangeReceiveGetResponse.Exchange> exchanges = Lists.newArrayList();
        for (; startIndex <= endIndex; startIndex++) {
            TmallExchangeReceiveGetResponse.Exchange exchange = mockExchange(now, startIndex);
            exchanges.add(exchange);
        }
        TmallExchangeReceiveGetResponse mockResp = new TmallExchangeReceiveGetResponse();
        mockResp.setHasNext(TOTAL > pageNo * pageSize);
        mockResp.setTotalResults(TOTAL);
        mockResp.setResults(exchanges);
        return mockResp;
    }

    private TmallExchangeReceiveGetResponse.Exchange mockExchange(Date now, Integer currentIndex) {
        TmallExchangeReceiveGetResponse.Exchange exchange = new TmallExchangeReceiveGetResponse.Exchange();
        String key = now.getTime() + "" + currentIndex;
        exchange.setDisputeId(key);
        exchange.setAlipayNo(key);
        exchange.setReason("想换就换");
        exchange.setCreated(now);
        exchange.setBoughtSku("1234566");
        exchange.setExchangeSku("6654321");
        exchange.setNum(3L);
        exchange.setBuyerAddress("浙江省^^^杭州市^^^西湖区^^^转塘街道");
        exchange.setBuyerName("mock_buyer");
        exchange.setBuyerPhone("13989489203");
        return exchange;
    }
}