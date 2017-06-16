package com.pousheng.middle.warehouses.algrithm;

import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import org.springframework.stereotype.Component;

/**
 *  根据收货地址选择仓库的算法
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@Component
public class WarehouseSelector {

    @RpcConsumer
    private WarehouseAddressRuleReadService warehouseAddressRuleReadService;

    //todo: 选择仓库
}
