package com.pousheng.middle.warehouse.service;

import com.google.common.base.Optional;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 仓库读服务
 * Date: 2017-06-07
 */
public interface WarehouseReadService {

    /**
     * 根据id查询仓库
     * @param Id 主键id
     * @return 仓库
     */
    Response<Warehouse> findById(Long Id);


    /**
     * 根据ids查询仓库集合
     * @param Ids 主键ids
     * @return 仓库
     */
    Response<List<Warehouse>> findByIds(List<Long> Ids);

    /**
     * 仓库列表
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数目
     * @param params 其他查询参数
     * @return 仓库列表
     */
    Response<Paging<Warehouse>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);

    /**
     * 根据仓库编码查询对应的仓库
     *
     * @param code 仓库编码
     * @return 对应的仓库
     */
    Response<Optional<Warehouse>> findByCode(String code);

    /**
     * 根据仓库编码模糊查询
     *
     * @param codePart 仓库编码部分
     * @return 匹配的仓库列表
     */
    Response<List<Warehouse>>  findByFuzzyCode(String codePart);

    /**
     * 仓库列表
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数目
     * @param name 名称或者外码
     * @return 仓库列表
     */
    Response<Paging<Warehouse>> pagingByOutCodeOrName(Integer pageNo, Integer pageSize,String name);
}
