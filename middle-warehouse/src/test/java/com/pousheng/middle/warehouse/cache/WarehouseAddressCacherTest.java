package com.pousheng.middle.warehouse.cache;

import com.pousheng.middle.warehouse.BaseServiceTest;
import com.pousheng.middle.warehouse.dto.AddressTree;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-09
 */
public class WarehouseAddressCacherTest extends BaseServiceTest{
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;

    @Test
    public void buildTree() throws Exception {
        AddressTree tree = warehouseAddressCacher.buildTree(2);
        assertThat(tree, notNullValue());
    }

}