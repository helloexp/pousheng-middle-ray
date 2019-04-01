package com.pousheng.middle.open;

import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 计算某个店铺中某个sku的可用库存
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-19
 */
@Component
@Slf4j
public class AvailableStockCalc {

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;

    private static final String WAREHOUSE_STATUS_ENABLED = "1";



}
