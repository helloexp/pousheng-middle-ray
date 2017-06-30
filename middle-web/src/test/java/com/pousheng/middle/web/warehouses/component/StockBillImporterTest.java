package com.pousheng.middle.web.warehouses.component;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-30
 */

@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest
@ActiveProfiles("webtest")
public class StockBillImporterTest {

    @Autowired
    private StockBillImporter stockBillImporter;

    @Test
    public void process() throws Exception {
        DateTime dateTime = DateTime.now().minusDays(30).withTimeAtStartOfDay();
        Date start  = dateTime.toDate();
        Date end = dateTime.plusHours(1).toDate();
        stockBillImporter.process(start, end);
    }

}