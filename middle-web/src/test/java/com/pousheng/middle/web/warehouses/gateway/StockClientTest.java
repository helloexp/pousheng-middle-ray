package com.pousheng.middle.web.warehouses.gateway;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.StockBill;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest
@ActiveProfiles("webtest")
public class StockClientTest {

    @Autowired
    private StockClient stockClient;


    @Test
    public void stockBills() throws Exception {

        Map<String, String > params = Maps.newHashMap();

        DateTime start = new DateTime().minusMonths(1);
        DateTime end = start.plusDays(1);
        List<StockBill> stockBills =  stockClient.stockBills("e-commerce-api/v1/hk-cgrk",
                start, end, 1, 20,params);
        System.out.println(stockBills);
    }

}