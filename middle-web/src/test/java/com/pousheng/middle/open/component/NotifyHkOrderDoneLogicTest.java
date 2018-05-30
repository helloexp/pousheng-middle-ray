/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: NotifyHkOrderDoneLogicTest
 * Author:   xiehong
 * Date:     2018/5/30 下午5:19
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.open.component;

import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.component.ReceiveYyediResultLogicTest;
import com.pousheng.middle.web.order.component.ReceiveYyediResultLogic;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.anyList;

/**
 * @author xiehong
 * @create 2018/5/30 下午5:19
 */
public class NotifyHkOrderDoneLogicTest extends AbstractRestApiTest {


    @Configuration
    public static class MockitoBeans {

        @MockBean
        private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
        @SpyBean
        private NotifyHkOrderDoneLogic notifyHkOrderDoneLogic;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    @Override
    protected void init() {
        poushengCompensateBizWriteService = get(PoushengCompensateBizWriteService.class);
        notifyHkOrderDoneLogic = get(NotifyHkOrderDoneLogic.class);

    }

    NotifyHkOrderDoneLogic notifyHkOrderDoneLogic;
    PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Test
    public void ctreateNotifyHkOrderDoneTask() {

        notifyHkOrderDoneLogic.ctreateNotifyHkOrderDoneTask(1234123L);

    }
}