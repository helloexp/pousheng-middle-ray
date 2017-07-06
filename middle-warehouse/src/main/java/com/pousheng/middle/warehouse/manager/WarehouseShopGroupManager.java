package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopGroupDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import io.terminus.common.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Component
public class WarehouseShopGroupManager {

    private final WarehouseShopGroupDao warehouseShopGroupDao;

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    @Autowired
    public WarehouseShopGroupManager(WarehouseShopGroupDao warehouseShopGroupDao,
                                     WarehouseRuleDao warehouseRuleDao,
                                     WarehouseAddressRuleDao warehouseAddressRuleDao) {
        this.warehouseShopGroupDao = warehouseShopGroupDao;
        this.warehouseRuleDao = warehouseRuleDao;
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
    }

    /**
     * 先创建店铺组, 然后创建一条默认发全国的规则, 并与该店铺组关联
     *
     * @param thinShops  店铺列表
     * @return 规则id
     */
    @Transactional
    public Long batchCreate(List<ThinShop> thinShops){

        Long nextShopGroupId = warehouseShopGroupDao.maxGroupId()+1;

        WarehouseRule warehouseRule = new WarehouseRule();
        warehouseRuleDao.create(warehouseRule);
        Long ruleId = warehouseRule.getId();

        for (ThinShop thinShop : thinShops) {
            WarehouseShopGroup wsr = new WarehouseShopGroup();
            BeanMapper.copy(thinShop, wsr);
            wsr.setGroupId(nextShopGroupId);
            warehouseShopGroupDao.create(wsr);
        }

        //为该店铺组创建一条默认发全国的规则
        WarehouseAddressRule warehouseAddressRule = new WarehouseAddressRule();
        warehouseAddressRule.setRuleId(ruleId);
        warehouseAddressRule.setAddressName("全国");
        warehouseAddressRule.setAddressId(1L);
        warehouseAddressRule.setShopGroupId(nextShopGroupId);
        warehouseAddressRuleDao.create(warehouseAddressRule);
        return ruleId;
    }

    /**
     * 更新店铺组对应的店铺信息
     *
     * @param groupId  组id
     * @param thinShops 店铺信息列表
     */
    @Transactional
    public void batchUpdate(Long groupId, List<ThinShop> thinShops){
        //首先清理掉原来店铺组对应的店铺信息
        warehouseShopGroupDao.deleteByGroupId(groupId);
        for (ThinShop thinShop : thinShops) {
            WarehouseShopGroup wsr = new WarehouseShopGroup();
            BeanMapper.copy(thinShop, wsr);
            wsr.setGroupId(groupId);
            warehouseShopGroupDao.create(wsr);
        }
    }

    /**
     * 删掉店铺组
     *
     * @param groupId 店铺组id
     */
    public void deleteByGroupId(Long groupId){
        warehouseShopGroupDao.deleteByGroupId(groupId);
    }

}
