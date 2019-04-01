package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleShop;
import io.terminus.common.model.Response;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

public interface ItemRuleShopWriteService {

    /**
     * 创建ItemRuleShop
     * @param itemRuleShop
     * @return 主键id
     */
    Response<Long> create(ItemRuleShop itemRuleShop);

    /**
     * 更新ItemRuleShop
     * @param itemRuleShop
     * @return 是否成功
     */
    Response<Boolean> update(ItemRuleShop itemRuleShop);

    /**
     * 根据主键id删除ItemRuleShop
     * @param itemRuleShopId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long itemRuleShopId);
}
