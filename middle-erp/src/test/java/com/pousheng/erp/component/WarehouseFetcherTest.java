package com.pousheng.erp.component;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class WarehouseFetcherTest {
    @Autowired
    private WarehouseFetcher warehouseFetcher;

    @Test
    public void fetch() throws Exception {

        System.out.println(warehouseFetcher.fetch(1,20, DateTime.now().minusMonths(1).toDate(),
                DateTime.now().minusMonths(1).plusDays(1).toDate()));
    }

}