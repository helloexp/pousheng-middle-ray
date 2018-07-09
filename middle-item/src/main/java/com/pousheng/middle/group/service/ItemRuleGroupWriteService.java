package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleGroup;
import io.terminus.common.model.Response;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

public interface ItemRuleGroupWriteService {

    /**
     * 创建ItemRuleGroup
     * @param itemRuleGroup
     * @return 主键id
     */
    Response<Long> create(ItemRuleGroup itemRuleGroup);

    /**
     * 更新ItemRuleGroup
     * @param itemRuleGroup
     * @return 是否成功
     */
    Response<Boolean> update(ItemRuleGroup itemRuleGroup);

    /**
     * 根据主键id删除ItemRuleGroup
     * @param itemRuleGroupId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long itemRuleGroupId);
}
