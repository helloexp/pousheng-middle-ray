package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.RefundWarehouseRules;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/8/26
 */
public interface RefundWarehouseRulesReadService {

    /**
     * 根据id查询售后指定退货仓
     * @param id
     * @return
     */
    Response<RefundWarehouseRules> findRulesById(Long id);

    /**
     * 根据销售店铺和发货仓账套查询
     * @param shopId
     * @param shipmentCompanyId
     * @return
     */
    Response<RefundWarehouseRules> findByShopIdAndShipmentCompanyId(Long shopId,String shipmentCompanyId);

    /**
     * 分页查询
     * @return
     */
    Response<Paging<RefundWarehouseRules>> pagingRules(Integer offset, Integer limit, RefundWarehouseRules criteria);
}
