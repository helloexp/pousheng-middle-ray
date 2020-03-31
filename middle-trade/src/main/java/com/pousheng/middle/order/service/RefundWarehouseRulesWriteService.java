package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.RefundWarehouseRules;
import io.terminus.common.model.Response;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/8/27
 */
public interface RefundWarehouseRulesWriteService {

    /**
     * 根据id删除规则信息
     * @param id
     * @return
     */
    Response<Boolean> deleteRulesById(Long id);

    /**
     * 创建规则信息
     * @param rules
     * @return
     */
    Response<Long> createRefundWarehouseRules(RefundWarehouseRules rules);

    /**
     * 修改规则信息
     * @param rules
     * @return
     */
    Response<Boolean> updateRefundWarehouseRules(RefundWarehouseRules rules);
}
