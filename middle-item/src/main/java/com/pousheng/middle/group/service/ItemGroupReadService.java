package com.pousheng.middle.group.service;

import com.pousheng.middle.group.dto.ItemGroupCriteria;
import com.pousheng.middle.group.model.ItemGroup;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */
public interface ItemGroupReadService {

    /**
     * 根据条件查询分组列表
     *
     * @param criteria 查询条件
     * @return 分页结果
     */
    Response<Paging<ItemGroup>> findByCriteria(ItemGroupCriteria criteria);

    /**
     * 根据id查看分组信息
     *
     * @param id 分组id
     * @return 分组信息
     */
    Response<ItemGroup> findById(Long id);

    /**
     * 根据id查看分组信息
     * @return 分组信息
     */
    Response<List<ItemGroup>> findAutoGroups();


    /**
     * 根据ids查看分组信息
     * @return 分组信息
     */
    Response<List<ItemGroup>> findByIds(List<Long> ids);



    /**
     * 检查分组名称重复
     *
     * @param name 分组名称
     * @return 分组id
     */
    Response<Boolean> checkName(String name);

    /**
     * 根据分组名称模糊查询
     * @param name
     * @return
     */
    Response<List<ItemGroup>> findByLikeName(String name);

}
