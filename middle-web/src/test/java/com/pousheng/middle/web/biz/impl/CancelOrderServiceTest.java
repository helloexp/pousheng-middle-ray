package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Optional;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2下午1:53
 */
public class CancelOrderServiceTest extends AbstractRestApiTest {


    @Configuration
    public static class MockitoBeans{

        @MockBean
        private OrderWriteLogic orderWriteLogic;

        @MockBean
        private ShopOrderReadService shopOrderReadService;

        @SpyBean
        private CancelOrderService cancelOrderService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    OrderWriteLogic orderWriteLogic;
    ShopOrderReadService shopOrderReadService;
    CancelOrderService cancelOrderService;


    @Override
    protected void init() {
        orderWriteLogic = get(OrderWriteLogic.class);
        shopOrderReadService = get(ShopOrderReadService.class);
        cancelOrderService = get(CancelOrderService.class);
    }

    @Test
    public void doBiz () {

        Response<Optional<ShopOrder>> resp = new Response<>();
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(123L);
        shopOrder.setOutFrom("yunjubbc");
        shopOrder.setOutId("486844183+100");
        Optional<ShopOrder> orderOptional = Optional.of(shopOrder);
        resp.setResult(orderOptional);

        when(shopOrderReadService.findByOutIdAndOutFrom(anyString(),anyString())).thenReturn(resp);

        String data = "{\"outOrderId\":\"486844183+100\",\"channel\":\"yunjunbbc\",\"applyAt\":\"20180912120000\"}";

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.OUTER_ORDER_CANCEL_RESULT.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        cancelOrderService.doProcess(biz);
    }

}
