package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopGroupDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@Component
public class WarehouseRuleManager {

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    private final WarehouseRuleItemDao warehouseRuleItemDao;

    private final WarehouseShopGroupDao warehouseShopGroupDao;

    @Autowired
    public WarehouseRuleManager(WarehouseRuleDao warehouseRuleDao,
                                WarehouseAddressRuleDao warehouseAddressRuleDao,
                                WarehouseRuleItemDao warehouseRuleItemDao,
                                WarehouseShopGroupDao warehouseShopGroupDao) {
        this.warehouseRuleDao = warehouseRuleDao;
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
        this.warehouseRuleItemDao = warehouseRuleItemDao;
        this.warehouseShopGroupDao = warehouseShopGroupDao;
    }

    @Transactional
    public void delete(Long warehouseRuleId) {
        warehouseRuleDao.delete(warehouseRuleId);
        warehouseAddressRuleDao.deleteByRuleId(warehouseRuleId);
        warehouseRuleItemDao.deleteByRuleId(warehouseRuleId);
        warehouseShopGroupDao.deleteByGroupId(warehouseRuleId);
    }
}
