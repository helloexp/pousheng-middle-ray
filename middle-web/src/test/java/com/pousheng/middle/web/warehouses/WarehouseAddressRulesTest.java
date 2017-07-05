package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.warehouse.dto.AddressTree;
import com.pousheng.middle.web.MiddleConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-10
 */
@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest(classes= MiddleConfiguration.class)
@ActiveProfiles("webtest")
public class WarehouseAddressRulesTest {

/*    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;*/

/*    @Test
    public void batchCreate() throws Exception {
    }*/

    @Autowired
    private WarehouseAddressRules warehouseAddressRules;

    @Test
    public void findAddressByRuleId() throws Exception {
        AddressTree actual = warehouseAddressRules.findAddressByRuleId(1L,1L);
        //System.out.println(actual);
        assertThat(actual, notNullValue());
    }

}