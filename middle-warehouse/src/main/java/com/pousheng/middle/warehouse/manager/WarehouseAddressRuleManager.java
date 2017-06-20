package com.pousheng.middle.warehouse.manager;

import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
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
     * 先创建规则, 然后为每一个地址创建一条关联记录
     *
     * @param thinAddresses  地址列表
     * @return 规则id
     */
    @Transactional
    public Long batchCreate(List<ThinAddress> thinAddresses){

        WarehouseRule warehouseRule = new WarehouseRule();
        warehouseRuleDao.create(warehouseRule);
        Long ruleId = warehouseRule.getId();

        for (ThinAddress thinAddress : thinAddresses) {
            WarehouseAddressRule war = new WarehouseAddressRule();
            BeanMapper.copy(thinAddress, war);
            war.setRuleId(ruleId);
            warehouseAddressRuleDao.create(war);
        }
        return ruleId;
    }

    /**
     * 更新规则对应的地址信息
     *
     * @param ruleId  规则id
     * @param thinAddresses 地址信息列表
     */
    @Transactional
    public void batchUpdate(Long ruleId, List<ThinAddress> thinAddresses){
        //首先清理掉原来规则对应的地址信息
        warehouseAddressRuleDao.deleteByRuleId(ruleId);
        for (ThinAddress thinAddress : thinAddresses) {
            WarehouseAddressRule war = new WarehouseAddressRule();
            BeanMapper.copy(thinAddress, war);
            war.setRuleId(ruleId);
            warehouseAddressRuleDao.create(war);
        }
    }

    /**
     * 删掉规则
     *
     * @param ruleId 规则id
     */
    public void deleteByRuleId(Long ruleId){
        warehouseAddressRuleDao.deleteByRuleId(ruleId);
    }

    /**
     * 删除对应的地址id
     *
     * @param id 地址对应的id
     * @param ruleId 规则id
     */
    public void deleteByIdAndRuleId(Long id, Long ruleId) {
        warehouseAddressRuleDao.deleteByIdAndRuleId(id, ruleId);

    }
}
