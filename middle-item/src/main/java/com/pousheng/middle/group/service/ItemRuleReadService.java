package com.pousheng.middle.group.service;

import com.pousheng.middle.group.dto.ItemRuleCriteria;
import com.pousheng.middle.group.model.ItemRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

public interface ItemRuleReadService {
    /**
     * 根据id查询
     * @param Id 主键id
     * @return
     */
    Response<ItemRule> findById(Long Id);

    /**
     * 分页查询规则详情，组装店铺信息和分组信息
     * @param criteria 查询条件
     * @return
     */
    Response<Paging<ItemRule>> paging(ItemRuleCriteria criteria);
}
