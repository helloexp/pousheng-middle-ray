package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseShopRule;
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
public class WarehouseShopRuleManager {

    private final WarehouseShopRuleDao warehouseShopRuleDao;

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    @Autowired
    public WarehouseShopRuleManager(WarehouseShopRuleDao warehouseShopRuleDao,
                                    WarehouseRuleDao warehouseRuleDao,
                                    WarehouseAddressRuleDao warehouseAddressRuleDao) {
        this.warehouseShopRuleDao = warehouseShopRuleDao;
        this.warehouseRuleDao = warehouseRuleDao;
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
    }

    /**
     * 先创建规则, 然后为每一个店铺创建一条关联记录, 并且创建一条默认发全国的规则
     *
     * @param thinShops  店铺列表
     * @return 规则id
     */
    @Transactional
    public Long batchCreate(List<ThinShop> thinShops){

        WarehouseRule warehouseRule = new WarehouseRule();
        warehouseRuleDao.create(warehouseRule);
        Long ruleId = warehouseRule.getId();

        for (ThinShop thinShop : thinShops) {
            WarehouseShopRule wsr = new WarehouseShopRule();
            BeanMapper.copy(thinShop, wsr);
            wsr.setRuleId(ruleId);
            warehouseShopRuleDao.create(wsr);

            //为该店铺创建一条默认发全国的规则
            WarehouseAddressRule warehouseAddressRule = new WarehouseAddressRule();
            warehouseAddressRule.setRuleId(ruleId);
            warehouseAddressRule.setAddressName("全国");
            warehouseAddressRule.setAddressId(1L);
            warehouseAddressRule.setShopId(thinShop.getShopId());
            warehouseAddressRuleDao.create(warehouseAddressRule);
        }
        return ruleId;
    }

    /**
     * 更新规则对应的店铺信息
     *
     * @param ruleId  规则id
     * @param thinShops 店铺信息列表
     */
    @Transactional
    public void batchUpdate(Long ruleId, List<ThinShop> thinShops){
        //首先清理掉原来规则对应的地址信息
        warehouseShopRuleDao.deleteByRuleId(ruleId);
        for (ThinShop thinShop : thinShops) {
            WarehouseShopRule wsr = new WarehouseShopRule();
            BeanMapper.copy(thinShop, wsr);
            wsr.setRuleId(ruleId);
            warehouseShopRuleDao.create(wsr);
        }
    }

    /**
     * 删掉规则
     *
     * @param ruleId 规则id
     */
    public void deleteByRuleId(Long ruleId){
        warehouseShopRuleDao.deleteByRuleId(ruleId);
    }

}
