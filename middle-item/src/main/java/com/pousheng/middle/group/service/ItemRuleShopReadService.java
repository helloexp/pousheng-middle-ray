package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleShop;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

public interface ItemRuleShopReadService {

    /**
     * 根据id查询
     * @param Id 主键id
     * @return
     */
    Response<ItemRuleShop> findById(Long Id);


    /**
     * 根据id查询
     * @param ruleId 规则id
     * @return
     */
    Response<List<ItemRuleShop>> findByRuleId(Long ruleId);

    /**
     * 根据id查询
     * @return
     */
    Response<List<Long>> findShopIds();

    /**
     * 检查店铺分组情况
     * @param ruleId 分组id
     * @param shopIds  店铺id
     * @return
     */
    Response<Boolean>  checkShopIds(Long ruleId,List<Long> shopIds);

    /**
     * 根据店铺id查询所属规则Id
     * @param shopId 店铺id
     * @return
     */
    Response<Long>  findRuleIdByShopId(Long shopId);
}
