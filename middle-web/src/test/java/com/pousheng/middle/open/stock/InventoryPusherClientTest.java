package com.pousheng.middle.open.stock;

import com.google.common.collect.Lists;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class InventoryPusherClientTest {

    @InjectMocks
    private InventoryPusherClient inventoryPusherClient;

    List<InventoryChangeDTO> changeDTOList;

    @Mock
    private ShopInventoryPusher shopInventoryPusher;

    @Mock
    private YjInventoryPusher yjInventoryPusher;

    @Before
    public void setUp() throws Exception {
        changeDTOList= Lists.newArrayList();

        changeDTOList.add(InventoryChangeDTO.builder().skuCode("A").warehouseId(1L).build());
    }

    @Test
    public void submit() {
        inventoryPusherClient.submit(Lists.newArrayList());

        inventoryPusherClient.submit(changeDTOList);
    }
}