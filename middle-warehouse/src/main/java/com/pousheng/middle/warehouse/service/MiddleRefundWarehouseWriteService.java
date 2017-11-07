package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/24
 * pousheng-middle
 */
public interface MiddleRefundWarehouseWriteService {
    /**
     * 更新退货仓
     * @param openShop
     * @return
     */
    Response<Boolean> update(OpenShop openShop);
}
