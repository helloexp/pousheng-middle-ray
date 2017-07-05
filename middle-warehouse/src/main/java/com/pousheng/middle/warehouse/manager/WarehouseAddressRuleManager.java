package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
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
public class WarehouseAddressRuleManager {

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    private final WarehouseRuleDao warehouseRuleDao;

    @Autowired
    public WarehouseAddressRuleManager(WarehouseAddressRuleDao warehouseAddressRuleDao,
                                       WarehouseRuleDao warehouseRuleDao) {
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
        this.warehouseRuleDao = warehouseRuleDao;
    }

    /**
     * 为每一个地址创建一条和规则关联的记录
     *
     * @param shopIds       店铺id
     * @param ruleId        规则id
     * @param thinAddresses 地址列表
     * @return 规则id
     */
    @Transactional
    public Long batchCreate(List<Long> shopIds, Long ruleId, List<ThinAddress> thinAddresses) {

        for (Long shopId : shopIds) {
            for (ThinAddress thinAddress : thinAddresses) {
                WarehouseAddressRule war = new WarehouseAddressRule();
                BeanMapper.copy(thinAddress, war);
                war.setRuleId(ruleId);
                war.setShopId(shopId);
                warehouseAddressRuleDao.create(war);
            }
        }
        return ruleId;
    }

    /**
     * 更新规则对应的地址信息
     *
     * @param shopIds       店铺id列表
     * @param ruleId        规则id
     * @param thinAddresses 地址信息列表
     */
    @Transactional
    public void batchUpdate(List<Long> shopIds, Long ruleId, List<ThinAddress> thinAddresses) {
        //首先清理掉原来规则对应的地址信息
        warehouseAddressRuleDao.deleteByRuleId(ruleId);
        for (Long shopId : shopIds) {
            for (ThinAddress thinAddress : thinAddresses) {
                WarehouseAddressRule war = new WarehouseAddressRule();
                BeanMapper.copy(thinAddress, war);
                war.setRuleId(ruleId);
                war.setShopId(shopId);
                warehouseAddressRuleDao.create(war);
            }
        }
    }

    /**
     * 删掉规则
     *
     * @param ruleId 规则id
     */
    public void deleteByRuleId(Long ruleId) {
        warehouseAddressRuleDao.deleteByRuleId(ruleId);
    }

}
