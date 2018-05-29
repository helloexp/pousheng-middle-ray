/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: ReceiveYyediResultLogicTest
 * Author:   xiehong
 * Date:     2018/5/29 下午10:39
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.component;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.order.component.ReceiveYyediResultLogic;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;

/**
 * 〈〉
 *
 * @author xiehong
 * @create 2018/5/29 下午10:39
 */
public class ReceiveYyediResultLogicTest extends AbstractRestApiTest {

    @Configuration
    public static class MockitoBeans {

        @MockBean
        private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
        @SpyBean
        private ReceiveYyediResultLogic receiveYyediResultLogic;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        poushengCompensateBizWriteService = get(PoushengCompensateBizWriteService.class);
        receiveYyediResultLogic = get(ReceiveYyediResultLogic.class);

    }

    ReceiveYyediResultLogic receiveYyediResultLogic;
    PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Test
    public void createShipmentResultTask(){

        receiveYyediResultLogic.createShipmentResultTask(anyList());

    }
}