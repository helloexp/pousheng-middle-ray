package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 公司规则读服务
 * Date: 2017-06-21
 */

public interface WarehouseCompanyRuleReadService {

    /**
     * 根据id查询店铺的公司规则
     * @param id 主键id
     * @return 店铺的公司规则
     */
    Response<WarehouseCompanyRule> findById(Long id);

    /**
     * 根据公司编码查找对应的公司规则
     *
     * @param companyCode 公司编码
     * @return 公司对应的规则
     */
    Response<WarehouseCompanyRule> findByCompanyCode(String companyCode);

    /**
     * 分页查找公司规则
     *
     * @param pageNo 起始页码
     * @param pageSize 每页显示条数
     * @param params 查询参数
     * @return 公司对应的规则
     */
    Response<Paging<WarehouseCompanyRule>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);

    /**
     * 获取所有的已设置规则的公司的编码
     *
     * @return 已设置规则的公司的编码
     */
    Response<List<String>> findCompanyCodes();
}
