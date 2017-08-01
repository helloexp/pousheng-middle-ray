package com.pousheng.middle.web.util.permission;

import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.web.MiddleConfiguration;
import com.pousheng.middle.web.order.Refunds;
import io.terminus.common.exception.JsonResponseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

/**
 * Created by sunbo@terminus.io on 2017/8/1.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleConfiguration.class)
@ActiveProfiles("webtest")
public class PermissionCheckTest {


    @Autowired
    private WebApplicationContext wac;

    @Test(expected = JsonResponseException.class)
    public void objectParameterTest() {

        Refunds refunds = wac.getBean(Refunds.class);

        SubmitRefundInfo info = new SubmitRefundInfo();
        info.setOrderId(4L);
        refunds.createRefund(info);
    }




}
