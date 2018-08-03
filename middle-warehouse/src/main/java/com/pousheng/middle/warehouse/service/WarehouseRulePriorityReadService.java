package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RulePriorityCriteria;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.Date;

/**
 * @author: zhaoxiaowei
 * Desc: 读服务
 * Date: 2018-09-04
 */

public interface WarehouseRulePriorityReadService {

    /**
     * 根据id查询
     *
     * @param id 主键id
     * @return
     */
    Response<WarehouseRulePriority> findById(Long id);

    /**
     * 根据条件分页查询
     *
     * @param criteria 查询条件
     * @return
     */
    Response<Paging<WarehouseRulePriority>> findByCriteria(RulePriorityCriteria criteria);

    /**
     * 检查名称重复
     *
     * @param warehouseRulePriority
     * @return
     */
    Response<Boolean> checkByName(WarehouseRulePriority warehouseRulePriority);


    /**
     * 检查有无时间区间重合
     *
     * @param warehouseRulePriority
     * @return
     */
    Response<Boolean> checkTimeRange(WarehouseRulePriority warehouseRulePriority);
}
