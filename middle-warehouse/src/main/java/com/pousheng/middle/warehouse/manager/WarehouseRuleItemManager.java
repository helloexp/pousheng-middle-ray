package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@Component
public class WarehouseRuleItemManager {

    private final WarehouseRuleItemDao warehouseRuleItemDao;
    private final WarehouseRuleDao warehouseRuleDao;

    @Autowired
    public WarehouseRuleItemManager(WarehouseRuleItemDao warehouseRuleItemDao, WarehouseRuleDao warehouseRuleDao) {
        this.warehouseRuleItemDao = warehouseRuleItemDao;
        this.warehouseRuleDao = warehouseRuleDao;
    }

    /**
     * 首先删除规则对应的仓库, 然后再添加新仓库
     *
     * @param ruleId 规则id
     * @param warehouseRuleItems  新仓库列表
     */
    @Transactional
    public void batchCreate(Long ruleId, WarehouseRuleItemPriorityType priorityType, List<WarehouseRuleItem> warehouseRuleItems){
        warehouseRuleItemDao.deleteByRuleId(ruleId);
        WarehouseRule warehouseRule = new WarehouseRule();
        warehouseRule.setId(ruleId);
        warehouseRule.setItemPriorityType(priorityType.value());
        warehouseRuleDao.update(warehouseRule);
        for (WarehouseRuleItem ruleItem : warehouseRuleItems) {
            warehouseRuleItemDao.create(ruleItem);
        }
    }
}
