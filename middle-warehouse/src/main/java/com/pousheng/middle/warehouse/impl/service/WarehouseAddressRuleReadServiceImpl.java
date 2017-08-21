package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.RuleSummary;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopGroupDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseAddressRuleReadServiceImpl implements WarehouseAddressRuleReadService {

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseRuleItemDao warehouseRuleItemDao;

    private final WarehouseShopGroupDao shopGroupDao;

    @Autowired
    public WarehouseAddressRuleReadServiceImpl(WarehouseAddressRuleDao warehouseAddressRuleDao,
                                               WarehouseRuleDao warehouseRuleDao,
                                               WarehouseRuleItemDao warehouseRuleItemDao,
                                               WarehouseShopGroupDao shopGroupDao) {
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
        this.warehouseRuleDao = warehouseRuleDao;
        this.warehouseRuleItemDao = warehouseRuleItemDao;
        this.shopGroupDao = shopGroupDao;
    }


    /**
     * 根据规则id查询地址和仓库规则的关联
     *
     * @param ruleId 规则id
     * @return 规则概述
     */
    @Override
    public Response<RuleSummary> findByRuleId(Long ruleId) {
        try {
            List<ThinAddress> addresses = doFindWarehouseAddressByRuleId(ruleId);

            WarehouseRule warehouseRule = warehouseRuleDao.findById(ruleId);
            if(warehouseRule == null){
                log.error("no WarehouseRule found by ruleId({})", ruleId);
                return Response.fail("rule.not.found");
            }
            RuleSummary ruleSummary = new RuleSummary();
            ruleSummary.setRuleId(ruleId);
            ruleSummary.setAddresses(addresses);
            return Response.ok(ruleSummary);
        }catch (ServiceException e){
            log.error("failed to find rule address information for rule(id={}), error code:{}", ruleId, e.getMessage());
            return Response.fail(e.getMessage());
        }catch(Exception e) {
            log.error("failed to find rule addresses information for ruleId({}), cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }

    private List<ThinAddress> doFindWarehouseAddressByRuleId(Long ruleId) {
        List<WarehouseAddressRule>  addressRules = warehouseAddressRuleDao.findByRuleId(ruleId);
        if(CollectionUtils.isEmpty(addressRules)){
            log.error("no WarehouseAddressRule found for ruleId({})", ruleId);
            throw new ServiceException("address.rule.not.found");
        }

        List<ThinAddress> addresses = Lists.newArrayListWithCapacity(addressRules.size());
        for (WarehouseAddressRule addressRule : addressRules) {
            ThinAddress thinAddress = new ThinAddress();
            BeanMapper.copy(addressRule, thinAddress);
            addresses.add(thinAddress);
        }
        return addresses;
    }

    /**
     * 根据仓库优先级规则id, 返回对应的仓库发货地址信息
     *
     * @param ruleId 规则id
     * @return 仓库发货地址信息
     */
    @Override
    public Response<List<ThinAddress>> findAddressByRuleId(Long ruleId) {
        try {
            List<ThinAddress> addresses = doFindWarehouseAddressByRuleId(ruleId);

            return Response.ok(addresses);
        } catch (ServiceException e){
            log.error("failed to find rule address information for rule(id={}), error code:{}", ruleId, e.getMessage());
            return Response.fail(e.getMessage());
        }catch(Exception e) {
            log.error("failed to find rule addresses information for ruleId({}), cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }

    /**
     * 查找店铺组所有其他非默认规则用掉的地址
     *
     * @param ruleId  除ruleId之外的规则id
     * @return 所有仓库发货地址集合
     */
    @Override
    public Response<List<ThinAddress>> findOtherNonDefaultAddressesByRuleId(Long ruleId) {
        try {
            WarehouseRule rule = warehouseRuleDao.findById(ruleId);
            if(rule == null){
                log.error("warehouse rule(id={}) not found", ruleId);
                return Response.fail("warehouse.rule.not.found");
            }
            Long shopGroupId = rule.getShopGroupId();
            List<WarehouseAddressRule>  addressRules = warehouseAddressRuleDao.findOtherNonDefaultRuleByShopGroupId(shopGroupId, ruleId);

            List<ThinAddress> addresses = Lists.newArrayListWithCapacity(addressRules.size());
            for (WarehouseAddressRule addressRule : addressRules) {
                ThinAddress thinAddress = new ThinAddress();
                BeanMapper.copy(addressRule, thinAddress);
                addresses.add(thinAddress);
            }
            return Response.ok(addresses);
        } catch (Exception e) {
            log.error("failed to find other rule addresses information in the same group as rule(id={}), cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }

    /**
     * 查找店铺组中发货规则已经用掉的地址
     *
     * @param shopGroupId 店铺组id
     * @return 对应的仓库发货地址集合
     */
    @Override
    public Response<List<ThinAddress>> findNonDefaultAddressesByShopGroupId(Long shopGroupId) {
        try {
            List<WarehouseAddressRule>  addressRules = warehouseAddressRuleDao.findNonDefaultRuleByShopGroupId(shopGroupId);

            List<ThinAddress> addresses = Lists.newArrayListWithCapacity(addressRules.size());
            for (WarehouseAddressRule addressRule : addressRules) {
                ThinAddress thinAddress = new ThinAddress();
                BeanMapper.copy(addressRule, thinAddress);
                addresses.add(thinAddress);
            }
            return Response.ok(addresses);
        } catch (Exception e) {
            log.error("failed to find rule addresses information in the shop group(id={}), cause:{}",
                    shopGroupId, Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }

    /**
     * 根据层级地址, 返回满足条件的仓库, 最精确的地址优先
     *
     * @param  shopId, 店铺id
     * @param addressIds 收货地址, 最精确的地址放在第一个,比如按照[区, 市, 省, 全国]的顺序传入
     * @return 所有能够发货到该地址的仓库列表
     */
    @Override
    public Response<List<Warehouses4Address>> findByReceiverAddressIds(Long shopId, List<Long> addressIds) {
        List<Warehouses4Address> candidates = Lists.newArrayList();
        List<WarehouseShopGroup> shopGroup = shopGroupDao.findByShopId(shopId);
        if(CollectionUtils.isEmpty(shopGroup)){
            log.error("no warehouse rule set for shop(id={})", shopId);
            return Response.fail("warehouse.rule.not.found");
        }
        Long shopGroupId = shopGroup.get(0).getGroupId();

        for (Long addressId : addressIds) {
            //首先根据地址找到符合对应的规则列表
            List<WarehouseAddressRule> rules = warehouseAddressRuleDao.findByShopGroupIdAndAddressId(shopGroupId, addressId);
            for (WarehouseAddressRule rule : rules) {
                //找到对应的规则细则
                List<WarehouseRuleItem> wris = warehouseRuleItemDao.findByRuleId(rule.getRuleId());
                if(CollectionUtils.isEmpty(wris)){
                    continue;
                }
                Warehouses4Address warehouses4Address = new Warehouses4Address();
                warehouses4Address.setAddressId(addressId);
                List<WarehouseWithPriority> wwps = Lists.newArrayListWithCapacity(wris.size());
                for (WarehouseRuleItem warehouseRuleItem : wris) {
                    WarehouseWithPriority wwp = new WarehouseWithPriority();
                    wwp.setPriority(warehouseRuleItem.getPriority());
                    wwp.setWarehouseId(warehouseRuleItem.getWarehouseId());
                    wwps.add(wwp);
                }
                warehouses4Address.setWarehouses(wwps);
                candidates.add(warehouses4Address);
            }
        }
        return Response.ok(candidates);
    }
}
