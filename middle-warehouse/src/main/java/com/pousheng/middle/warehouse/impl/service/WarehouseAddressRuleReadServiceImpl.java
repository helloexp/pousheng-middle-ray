package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public WarehouseAddressRuleReadServiceImpl(WarehouseAddressRuleDao warehouseAddressRuleDao) {
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
    }

    @Override
    public Response<WarehouseAddressRule> findById(Long Id) {
        try {
            return Response.ok(warehouseAddressRuleDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseAddressRule by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.rule.find.fail");
        }
    }
}
