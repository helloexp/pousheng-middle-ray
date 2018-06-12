package com.pousheng.middle.web.order.component;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.RefundAmountWriteService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class RefundWriteLogicTest extends AbstractRestApiTest {

    private RefundWriteLogic refundWriteLogic;
    private RefundReadLogic refundReadLogic;
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    private ShipmentWiteLogic shipmentWiteLogic;
    private ShipmentReadLogic shipmentReadLogic;

    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private RefundWriteLogic refundWriteLogic;
        @MockBean
        private RefundReadLogic refundReadLogic;
        @RpcConsumer
        private RefundWriteService refundWriteService;
        @RpcConsumer
        private SkuTemplateReadService skuTemplateReadService;
        @MockBean
        private ShipmentWiteLogic shipmentWiteLogic;
        @MockBean
        private ShipmentReadLogic shipmentReadLogic;
        @MockBean
        private OrderReadLogic orderReadLogic;
        @MockBean
        private MiddleOrderFlowPicker flowPicker;
        @MockBean
        private MiddleRefundWriteService middleRefundWriteService;
        @MockBean
        private WarehouseReadService warehouseReadService;
        @MockBean
        private SyncErpReturnLogic syncErpReturnLogic;
        @RpcConsumer
        private ShipmentReadService shipmentReadService;
        @MockBean
        private PoushengSettlementPosReadService poushengSettlementPosReadService;
        @MockBean
        private RefundAmountWriteService refundAmountWriteService;
        @MockBean
        private EventBus eventBus;
        @MockBean
        private SyncRefundLogic syncRefundLogic;
        private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
        @MockBean
        private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
        
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    public void init() {
        this.refundWriteLogic = get(RefundWriteLogic.class);
        this.refundReadLogic = get(RefundReadLogic.class);
        this.poushengCompensateBizWriteService = get(PoushengCompensateBizWriteService.class);
        this.shipmentReadLogic = get(ShipmentReadLogic.class);
        this.shipmentWiteLogic = get(ShipmentWiteLogic.class);
    }

    @Test
    public void getThirdRefundResult() {
        Refund refund = new Refund();
        String channle = MiddleChannel.TAOBAO.getValue();
        Refund newRefund = new Refund();
        OrderRefund orderRefund = new OrderRefund();

        when(refundReadLogic.getOutChannelTaobao(any())).thenReturn(channle);
        when(refundReadLogic.findRefundById(any())).thenReturn(newRefund);
        when(refundReadLogic.getOutChannelSuning(any())).thenReturn(channle);
        when(refundReadLogic.findOrderRefundByRefundId(any())).thenReturn(orderRefund);
        when(poushengCompensateBizWriteService.create(any())).thenReturn(null);
        refundWriteLogic.getThirdRefundResult(refund);
    }

    @Test
    public void createRefundResultTask() {
        Refund refund = new Refund();
        refund.setId(100l);
        String channel = MiddleChannel.TAOBAO.getValue();
        Refund newRefund = new Refund();
        newRefund.setShopId(100l);
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setShopId(100l);
        TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
        event.setRefundId(refund.getId());
        event.setChannel(channel);
        event.setOpenShopId(newRefund.getShopId());
        event.setOpenOrderId(shopOrder.getOutId());
        ReflectionTestUtils.invokeMethod(refundWriteLogic,"createRefundResultTask", event);
    }
}
