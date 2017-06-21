package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.RuleDto;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleDao;
import com.pousheng.middle.warehouse.impl.dao.WarehouseRuleItemDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Author: jlchen
 * Desc: 仓库优先级规则概述读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseRuleReadServiceImpl implements WarehouseRuleReadService {

    private final WarehouseRuleDao warehouseRuleDao;

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    private final WarehouseRuleItemDao warehouseRuleItemDao;

    @Autowired
    public WarehouseRuleReadServiceImpl(WarehouseRuleDao warehouseRuleDao,
                                        WarehouseAddressRuleDao warehouseAddressRuleDao,
                                        WarehouseRuleItemDao warehouseRuleItemDao) {
        this.warehouseRuleDao = warehouseRuleDao;
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
        this.warehouseRuleItemDao = warehouseRuleItemDao;
    }

    @Override
    public Response<WarehouseRule> findById(Long Id) {
        try {
            return Response.ok(warehouseRuleDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseRule by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.find.fail");
        }
    }

    /**
     * 分页查看规则概览
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回条数
     * @return 仓库规则概述
     */
    @Override
    public Response<Paging<RuleDto>> pagination(Integer pageNo, Integer pageSize) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<WarehouseRule> p = warehouseRuleDao.paging(pageInfo.getOffset(), pageInfo.getLimit());
            if(CollectionUtils.isEmpty(p.getData())){
                return Response.ok(new Paging<>(p.getTotal(), Collections.emptyList()));
            }
            List<WarehouseRule> warehouseRules = p.getData();
            List<RuleDto> ruleDtos = Lists.newArrayListWithCapacity(p.getData().size());
            for (WarehouseRule warehouseRule : warehouseRules) {
                Long ruleId = warehouseRule.getId();
                AddressesAndLastUpdatedAt addressesAndLastUpdatedAt = doFindWarehouseAddressByRuleId(ruleId);
                List<WarehouseRuleItem> ruleItems = warehouseRuleItemDao.findByRuleId(ruleId);
                RuleDto ruleDto = new RuleDto();
                ruleDto.setRuleId(ruleId);
                ruleDto.setAddresses(addressesAndLastUpdatedAt.getAddresses());
                ruleDto.setRuleItems(ruleItems);
                Date updatedAt = addressesAndLastUpdatedAt.getUpdatedAt();
                if(!CollectionUtils.isEmpty(ruleItems) && ruleItems.get(0).getCreatedAt().after(updatedAt)){
                    updatedAt = ruleItems.get(0).getCreatedAt();
                }
                ruleDto.setUpdatedAt(updatedAt);
                ruleDtos.add(ruleDto);
            }
            Paging<RuleDto> r = new Paging<>(p.getTotal(), ruleDtos);
            return Response.ok(r);
        } catch (Exception e) {
            log.error("failed to pagination rule summary, cause:{}",Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.rule.find.fail");
        }
    }

    /**
     * 同时返回最新更新时间及地址列表
     */
    private AddressesAndLastUpdatedAt doFindWarehouseAddressByRuleId(Long ruleId) {
        List<WarehouseAddressRule>  addressRules = warehouseAddressRuleDao.findByRuleId(ruleId);
        if(CollectionUtils.isEmpty(addressRules)){
            log.error("no WarehouseAddressRule found for ruleId({})", ruleId);
            throw new ServiceException("address.rule.not.found");
        }

        Date updatedAt = addressRules.get(0).getUpdatedAt();
        List<ThinAddress> addresses = Lists.newArrayListWithCapacity(addressRules.size());
        for (WarehouseAddressRule addressRule : addressRules) {
            if(addressRule.getUpdatedAt().after(updatedAt)){
                updatedAt = addressRule.getUpdatedAt();
            }
            ThinAddress thinAddress = new ThinAddress();
            BeanMapper.copy(addressRule, thinAddress);
            addresses.add(thinAddress);
        }
        return new AddressesAndLastUpdatedAt(addresses, updatedAt);
    }

    /**
     * 同时返回最新更新时间及地址列表
     */
    private static class AddressesAndLastUpdatedAt implements Serializable{

        private static final long serialVersionUID = 3879361032466009274L;

        @Getter
        private final Date updatedAt;

        @Getter
        private final List<ThinAddress> addresses;

        public AddressesAndLastUpdatedAt(List<ThinAddress> addresses, Date updatedAt) {
            this.updatedAt = updatedAt;
            this.addresses = addresses;
        }
    }


}
