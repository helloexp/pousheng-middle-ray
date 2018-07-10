package com.pousheng.middle.group.service;

import com.pousheng.middle.group.model.ItemRuleWarehouse;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表写服务
 * Date: 2018-07-13
 */

public interface ItemRuleWarehouseWriteService {

    /**
     * 创建ItemRuleWarehouse
     * @param itemRuleWarehouse
     * @return 主键id
     */
    Response<Long> create(ItemRuleWarehouse itemRuleWarehouse);

    /**
     * 更新ItemRuleWarehouse
     * @param itemRuleWarehouse
     * @return 是否成功
     */
    Response<Boolean> update(ItemRuleWarehouse itemRuleWarehouse);

    /**
     * 根据主键id删除ItemRuleWarehouse
     * @param itemRuleWarehouseId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long itemRuleWarehouseId);
}
