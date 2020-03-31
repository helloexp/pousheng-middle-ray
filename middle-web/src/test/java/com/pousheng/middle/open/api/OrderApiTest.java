package com.pousheng.middle.open.api;

import com.pousheng.middle.web.MiddleConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author feisheng.ch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MiddleConfiguration.class)
@ActiveProfiles("test")
public class OrderApiTest {

    @Autowired
    private OrderOpenApi orderOpenApi;


    @Test
    public void testDoneShipment() {

        orderOpenApi.syncHkShipmentStatus("SHP102871","110","JDCOD","SERIAL0001", "20180531121212");

    }

}
