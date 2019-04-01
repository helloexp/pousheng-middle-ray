package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author: zhaoxiaowei
 * Desc: 读服务
 * Date: 2018-09-04
 */

public interface WarehouseRulePriorityItemReadService {

    /**
     * 根据id查询
     *
     * @param id 主键id
     * @return
     */
    Response<WarehouseRulePriorityItem> findById(Long id);

    /**
     * 根据信息查询
     *
     * @param item
     * @return
     */
    Response<WarehouseRulePriorityItem> findByEntity(WarehouseRulePriorityItem item);

    /**
     * 根据优先级规则id查询明细
     * @param priorityId
     * @return
     */
    Response<List<WarehouseRulePriorityItem>> findByPriorityId(Long priorityId);
}
