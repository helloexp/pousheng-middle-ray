package com.pousheng.middle.web.trade;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.web.MiddleConfiguration;
import io.terminus.open.client.order.dto.OpenClientOrderShipment;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/2
 * pousheng-middle
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MiddleConfiguration.class)
@ActiveProfiles("webtest")
public class ApiTest {

    @Autowired
    private InventoryClient inventoryClient;

    @Test
    public void test(){
        inventoryClient.getAvailableInventory(Lists.newArrayList(AvailableInventoryRequest.builder().warehouseId(1L).skuCode("88888").build()), 8L);
    }

}
