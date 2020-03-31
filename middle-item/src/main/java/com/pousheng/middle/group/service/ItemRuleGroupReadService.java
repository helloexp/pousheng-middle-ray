package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleGroup;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

public interface ItemRuleGroupReadService {

    /**
     * 根据id查询
     * @param Id 主键id
     * @return
     */
    Response<ItemRuleGroup> findById(Long Id);


    /**
     * 根据id查询
     * @param ruleId  规则id
     * @return
     */
    Response<List<ItemRuleGroup>> findByRuleId(Long ruleId);


    /**
     * 根据id查询
     * @param groupId  分组id
     * @return
     */
    Response<List<ItemRuleGroup>> findByGroupId(Long groupId);
}
