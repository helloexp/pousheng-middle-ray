package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.dto.RuleGroup;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

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
     * @return 按照店铺组id归组的仓库规则概述
     */
    Response<Paging<RuleGroup>> pagination(Integer pageNo, Integer pageSize);


    /**
     * 根据店铺id查找设置的仓库列表
     *
     * @param shopId  店铺id
     * @return 仓库列表
     */
    Response<List<Long>>  findWarehouseIdsByShopId(Long shopId);

}
