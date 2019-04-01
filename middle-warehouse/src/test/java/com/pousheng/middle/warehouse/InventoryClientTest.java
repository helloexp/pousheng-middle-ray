package com.pousheng.middle.warehouse;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.companent.InventoryBaseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class InventoryClientTest {

    @Autowired
    private InventoryBaseClient inventoryClient;

    @Test
    public void get() throws Exception {

        Map<String, Object> params = Maps.newHashMap();

        WarehouseDTO stockBills = (WarehouseDTO) inventoryClient.get("api/inventory/warehouse/1",
                1, 20,params, WarehouseDTO.class, false);
        System.out.println(stockBills);
    }

}