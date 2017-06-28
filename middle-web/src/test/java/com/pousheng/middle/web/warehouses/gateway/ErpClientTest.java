package com.pousheng.middle.web.warehouses.gateway;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.StockBill;
import com.pousheng.middle.web.erp.ErpClient;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest
@ActiveProfiles("webtest")
public class ErpClientTest {

    @Autowired
    private ErpClient erpClient;


    @Test
    public void stockBills() throws Exception {

        Map<String, String > params = Maps.newHashMap();

        DateTime start = new DateTime().minusMonths(1);
        DateTime end = start.plusDays(1);
        List<StockBill> stockBills =  erpClient.stockBills("e-commerce-api/v1/hk-cgrk",
                start, end, 1, 20,params);
        System.out.println(stockBills);
    }

}