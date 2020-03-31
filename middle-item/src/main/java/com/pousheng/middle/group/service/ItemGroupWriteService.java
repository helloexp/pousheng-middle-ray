package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import io.terminus.common.model.Response;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */
public interface ItemGroupWriteService {

    /**
     * 创建商品分组
     *
     * @param itemGroup 分组信息
     * @return 分组id
     */
    Response<Long> create(ItemGroup itemGroup);

    /**
     * 编辑商品分组
     *
     * @param itemGroup 分组信息
     * @return 是否成功
     */
    Response<Boolean> update(ItemGroup itemGroup);

    /**
     * 编辑自动分组规则
     * @param groupId
     * @param rule
     * @return
     */
    Response<Boolean> updateAutoRule(Long groupId,ItemGroupAutoRule rule);

    /**
     * 根据分组ID删除分组
     *
     * @param id 分组id
     * @return 是否成功
     */
    Response<Boolean> delete(Long id);


}
