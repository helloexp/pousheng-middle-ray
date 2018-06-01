package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.AfterSaleServiceRegistryCenter;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.service.OpenClientAfterSaleService;
import io.terminus.parana.order.service.RefundWriteService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ThirdRefundResultServiceTest extends AbstractRestApiTest {
    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private ThirdRefundResultService thirdRefundResultService;
        @MockBean
        private AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;
        @MockBean
        private RefundWriteService refundWriteService;
        @MockBean
        private RefundReadLogic refundReadLogic;
        @MockBean
        private OpenClientAfterSaleService afterSaleService;
    }

    ThirdRefundResultService thirdRefundResultService;
    AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;
    RefundWriteService refundWriteService;
    RefundReadLogic refundReadLogic;
    OpenClientAfterSaleService afterSaleService;

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Before
    public void init() {
        this.thirdRefundResultService = get(ThirdRefundResultService.class);
        this.afterSaleServiceRegistryCenter = get(AfterSaleServiceRegistryCenter.class);
        this.refundWriteService = get(RefundWriteService.class);
        this.refundReadLogic = get(RefundReadLogic.class);
        this.afterSaleService = get(OpenClientAfterSaleService.class);
    }

    @Test
    public void doProcess() {
        Response<OpenClientAfterSale> resp = new Response<OpenClientAfterSale>();
        OpenClientAfterSale openClientAfterSale = new OpenClientAfterSale();
        openClientAfterSale.setOpenOrderId("100");
        resp.setResult(openClientAfterSale);

        Response<Boolean> updateR = new Response<Boolean>();
        Boolean result = new Boolean(true);
        updateR.setResult(result);

        when(afterSaleServiceRegistryCenter.getAfterSaleService(anyString())).thenReturn(afterSaleService);
        when(afterSaleService.findByAfterSaleId(any(),any())).thenReturn(resp);
        when(refundWriteService.updateStatus(any(),any())).thenReturn(updateR);

        TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
        event.setChannel(MiddleChannel.TAOBAO.getValue());
        event.setOpenShopId(200l);
        event.setRefundId(100l);
        event.setOpenAfterSaleId("order");
        event.setOpenOrderId("sale");
        String jsonStr = JsonMapper.nonEmptyMapper().toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.THIRD_REFUND_RESULT.toString());
        biz.setContext(jsonStr);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        thirdRefundResultService.doProcess(biz);
    }
}