package com.pousheng.erp.component;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class ErpClientTest {

    @Autowired
    private ErpClient erpClient;


    @Test
    public void get() throws Exception {

        Map<String, String > params = Maps.newHashMap();

        Date start = DateTime.now().minusMonths(1).toDate();
        Date end = DateTime.now().plusDays(1).toDate();
        String stockBills =  erpClient.get("e-commerce-api/v1/hk-cgrk",
                start, end, 1, 20,params);
        System.out.println(stockBills);
    }

}