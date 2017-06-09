package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.dto.WarehouseAddressDto;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
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

    @Autowired
    public WarehouseAddressRuleReadServiceImpl(WarehouseAddressRuleDao warehouseAddressRuleDao,
                                               WarehouseRuleDao warehouseRuleDao) {
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
        this.warehouseRuleDao = warehouseRuleDao;
    }


    /**
     * 根据规则id查询地址和仓库规则的关联
     *
     * @param ruleId 规则id
     * @return 规则概述
     */
    @Override
    public Response<RuleDto> findByRuleId(Long ruleId) {
        try {
            List<WarehouseAddressDto> addresses = doFindWarehouseAddressByRuleId(ruleId);

            WarehouseRule warehouseRule = warehouseRuleDao.findById(ruleId);
            if(warehouseRule == null){
                log.error("no WarehouseRule found by ruleId({})", ruleId);
                return Response.fail("rule.not.found");
            }
            RuleDto ruleDto = new RuleDto();
            ruleDto.setRuleId(ruleId);
            ruleDto.setRuleDesc(warehouseRule.getName());
            ruleDto.setAddresses(addresses);
            return Response.ok(ruleDto);
        }catch (ServiceException e){
            log.error("failed to find rule address information for rule(id={}), error code:{}", ruleId, e.getMessage());
            return Response.fail(e.getMessage());
        }catch(Exception e) {
            log.error("failed to find rule addresses information for ruleId({}), cause:{}",
                    ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }

    private List<WarehouseAddressDto> doFindWarehouseAddressByRuleId(Long ruleId) {
        List<WarehouseAddressRule>  addressRules = warehouseAddressRuleDao.findByRuleId(ruleId);
        if(CollectionUtils.isEmpty(addressRules)){
            log.error("no WarehouseAddressRule found for ruleId({})", ruleId);
            throw new ServiceException("address.rule.not.found");
        }

        List<WarehouseAddressDto> addresses = Lists.newArrayListWithCapacity(addressRules.size());
        for (WarehouseAddressRule addressRule : addressRules) {
            WarehouseAddressDto warehouseAddressDto = new WarehouseAddressDto();
            BeanMapper.copy(addressRule, warehouseAddressDto);
            addresses.add(warehouseAddressDto);
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
    public Response<List<WarehouseAddressDto>> findAddressByRuleId(Long ruleId) {
        try {
            List<WarehouseAddressDto> addresses = doFindWarehouseAddressByRuleId(ruleId);

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
}
