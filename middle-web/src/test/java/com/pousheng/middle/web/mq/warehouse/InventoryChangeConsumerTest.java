package com.pousheng.middle.web.mq.warehouse;

import com.google.common.collect.Lists;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class InventoryChangeConsumerTest {

    @InjectMocks
    private InventoryChangeConsumer inventoryChangeConsumer;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void handleInventoryChange() {
    }

    @Test
    public void validateParam() {
        List<InventoryChangeDTO> list= Lists.newArrayList();
        list.add(InventoryChangeDTO.builder().skuCode("A").build());
        list.add(InventoryChangeDTO.builder().skuCode("B").warehouseId(1L).build());
        list.add(InventoryChangeDTO.builder().skuCode("C").shopId(1L).build());
        boolean result=inventoryChangeConsumer.validateParam(list);

        Assert.assertFalse(result);

        List<InventoryChangeDTO> list1= Lists.newArrayList();
        list1.add(InventoryChangeDTO.builder().skuCode("A").warehouseId(1L).build());
        list1.add(InventoryChangeDTO.builder().skuCode("B").warehouseId(1L).build());
        list1.add(InventoryChangeDTO.builder().skuCode("C").shopId(1L).build());
        result=inventoryChangeConsumer.validateParam(list1);

        Assert.assertTrue(result);
    }
}