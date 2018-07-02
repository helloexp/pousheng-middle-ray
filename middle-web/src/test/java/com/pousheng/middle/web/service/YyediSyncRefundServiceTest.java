/**
 * Copyright (C), 2012-2018, XXX有限公司
 */
package com.pousheng.middle.web.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.impl.YyediSyncRefundService;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * 〈〉
 *
 * Author: xiehong (168479)
 * Date: 2018/5/31 下午3:24
 */
public class YyediSyncRefundServiceTest extends AbstractRestApiTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private RefundWriteLogic refundWriteLogic;
        @MockBean
        private AutoCompensateLogic autoCompensateLogic;
        @MockBean
        private SyncRefundPosLogic syncRefundPosLogic;

        @SpyBean
        private YyediSyncRefundService yyediSyncRefundService;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        refundWriteLogic = get(RefundWriteLogic.class);
        autoCompensateLogic = get(AutoCompensateLogic.class);
        syncRefundPosLogic = get(SyncRefundPosLogic.class);
        yyediSyncRefundService = get(YyediSyncRefundService.class);

    }

    RefundWriteLogic refundWriteLogic;
    AutoCompensateLogic autoCompensateLogic;
    SyncRefundPosLogic syncRefundPosLogic;
    YyediSyncRefundService yyediSyncRefundService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Test
    public void doProcess() {

        when(refundWriteLogic.update(any())).thenReturn(Response.ok());
        when(syncRefundPosLogic.syncRefundPosToHk(any())).thenReturn(Response.ok());
        // doNothing().when(autoCompensateLogic.createAutoCompensationTask(anyMap(),anyInt(),anyString()));
        Refund refund = new Refund();
        refund.setId(123L);
        refund.setReleOrderCode("456");
        YYEdiRefundConfirmItem item = new YYEdiRefundConfirmItem();
        item.setItemCode("1000221232312");
        item.setWarhouseCode("r123");
        item.setQuantity("31");
        List<YYEdiRefundConfirmItem> items = Lists.newArrayList();
        items.add(item);
        Map<String, String> extra = Maps.newHashMap();
        extra.put(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO, mapper.toJson(items));
        try {
            refund.setExtraJson(mapper.toJson(extra));
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Refund> refunds = Lists.newArrayList();
        refunds.add(refund);

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.YYEDI_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext(mapper.toJson(refunds));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        yyediSyncRefundService.doProcess(biz);

    }



}