package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述读服务
 * Date: 2017-06-07
 */

public interface WarehouseRuleReadService {

    /**
     * 根据id查询仓库优先级规则概述
     * @param Id 主键id
     * @return 仓库优先级规则概述
     */
    Response<WarehouseRule> findById(Long Id);


    /**
     * 分页查看规则概览
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回条数
     * @return 仓库规则概述
     */
    Response<Paging<RuleDto>> pagination(Integer pageNo, Integer pageSize);

}
