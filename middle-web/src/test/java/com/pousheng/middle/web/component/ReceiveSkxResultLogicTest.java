package com.pousheng.middle.web.component;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.order.component.ReceiveSkxResultLogic;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.anyObject;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2上午11:40
 */
public class ReceiveSkxResultLogicTest extends AbstractRestApiTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
        @SpyBean
        private ReceiveSkxResultLogic receiveSkxResultLogic;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }


    PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    ReceiveSkxResultLogic receiveSkxResultLogic;

    @Override
    protected void init() {
        poushengCompensateBizWriteService = get(PoushengCompensateBizWriteService.class);
        receiveSkxResultLogic = get(ReceiveSkxResultLogic.class);

    }

    @Test
    public void createShipmentResultTask() {
        receiveSkxResultLogic.createShipmentResultTask(anyObject());
    }

}
