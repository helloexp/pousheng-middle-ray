package com.pousheng.middle.warehouse.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/24
 * pousheng-middle
 */
public interface MiddleRefundWarehouseReadService {

    Response<Paging<OpenShop>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);
}
