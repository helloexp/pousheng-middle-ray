package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRule;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
public interface ItemRuleWriteService {

    /**
     * 创建ItemRule
     *
     * @param itemRule
     * @return 主键id
     */
    Response<Long> create(ItemRule itemRule);

    /**
     * 更新ItemRule
     *
     * @param itemRule
     * @return 是否成功
     */
    Response<Boolean> update(ItemRule itemRule);

    /**
     * 根据主键id删除ItemRule
     *
     * @param itemRuleId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long itemRuleId);


    /**
     * 初始化规则的店铺信息
     * @param shopIds 店铺ids
     * @return
     */
    Response<Long> createWithShop(List<Long> shopIds);


    /**
     * 更新规则的店铺
     * @param ruleId 规则id
     * @param shopIds 店铺ids
     * @return
     */
    Response<Boolean> updateShops(Long ruleId, List<Long> shopIds);


    /**
     * 更新规则的分组
     * @param ruleId 规则id
     * @param groupIds 分组ids
     * @return
     */
    Response<Boolean> updateGroups(Long ruleId, List<Long> groupIds);


    /**
     * 删除规则及所有的映射关系
     * @param itemRuleId 规则id
     * @return
     */
    Response<Boolean> delete(Long itemRuleId);

}

