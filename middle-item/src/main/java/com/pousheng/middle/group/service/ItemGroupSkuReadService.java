package com.pousheng.middle.group.service;

import com.pousheng.middle.group.dto.ItemGroupSkuCriteria;
import com.pousheng.middle.group.model.ItemGroupSku;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

public interface ItemGroupSkuReadService {

    /**
     * 根据分组id获取所属商品信息
     * @param groupId
     * @return
     */
    Response<List<ItemGroupSku>> findByGroupId(Long groupId);

    /**
     * 根据分组id和商品类型获取所属信息
     * @param groupId
     * @param type
     * @return
     */
    Response<List<ItemGroupSku>> findByGroupIdAndType(Long groupId, Integer type);

    /**
     * 根据条件查询列表
     * @param criteria
     * @return 分页结果
     */
    Response<Paging<ItemGroupSku>> findByCriteria(ItemGroupSkuCriteria criteria);

    /**
     * 获取总数
     * @param groupId
     * @param type
     * @return
     */
    Response<Long> count(Long groupId, Integer type);
}
