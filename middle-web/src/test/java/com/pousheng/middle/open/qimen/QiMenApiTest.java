package com.pousheng.middle.open.qimen;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.open.ReceiverInfoCompleter;
import com.pousheng.middle.open.erp.ErpOpenApiClient;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

public class QiMenApiTest extends AbstractRestApiTest {

    private QiMenApi qiMenApi;
    private ShopOrderReadService shopOrderReadService;
    private MiddleOrderWriteService middleOrderWriteService;
    private ErpOpenApiClient erpOpenApiClient;
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Configuration
    public static class MockitoBeans {
        @SpyBean
        QiMenApi qiMenApi;
        @MockBean
        ShopOrderReadService shopOrderReadService;
        @MockBean
        ReceiverInfoCompleter receiverInfoCompleter;
        @MockBean
        EventBus eventBus;
        @MockBean
        ErpOpenApiClient erpOpenApiClient;
        @MockBean
        PoushengCompensateBizWriteService poushengCompensateBizWriteService;
        @MockBean
        MiddleOrderWriteService middleOrderWriteService;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        qiMenApi = get(QiMenApi.class);
        shopOrderReadService = get(ShopOrderReadService.class);
        middleOrderWriteService = get(MiddleOrderWriteService.class);
    }

    @Test
    public void createShipmentResultTask(){
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(100l);
        OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(shopOrder.getId());
        ReflectionTestUtils.invokeMethod(qiMenApi,"createShipmentResultTask", event);
    }
}