package com.pousheng.middle.open.stock;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YjInventoryPusherTest {

    @InjectMocks
    private YjInventoryPusher yjInventoryPusher;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void push() {
        yjInventoryPusher.push(Lists.newArrayList());
    }

    @Test
    public void push1() {
    }
}