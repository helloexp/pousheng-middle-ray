package com.pousheng.middle.hksyc.component;

import com.pousheng.middle.hksyc.dto.JitOrderReceiptRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
//@PrepareForTest(HttpRequest.class)
public class JitOrderReceiptApiTest {

    @InjectMocks
    private JitOrderReceiptApi jitOrderReceiptApi;

    @Test
    public void sendReceipt() {
        String successStr="{\"error\":0,\"error_info\":\"\",\"data\":[]}";
        Long shopId = 1l;
        //PowerMockito.mockStatic(HttpRequest.class);
        //when(HttpRequest.post(anyString()).body()).thenReturn(successStr);
        jitOrderReceiptApi.sendReceipt(new JitOrderReceiptRequest(), shopId);
    }
}