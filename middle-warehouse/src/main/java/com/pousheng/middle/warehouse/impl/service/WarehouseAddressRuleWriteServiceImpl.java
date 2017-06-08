package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseAddressRule;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 地址和仓库规则的关联写服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseAddressRuleWriteServiceImpl implements WarehouseAddressRuleWriteService {

    private final WarehouseAddressRuleDao warehouseAddressRuleDao;

    @Autowired
    public WarehouseAddressRuleWriteServiceImpl(WarehouseAddressRuleDao warehouseAddressRuleDao) {
        this.warehouseAddressRuleDao = warehouseAddressRuleDao;
    }

    /**
     * 创建WarehouseAddresses
     *
     * @param warehouseAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    @Override
    public Response<Long> batchCreate(List<WarehouseAddress> warehouseAddresses) {
        return null;
    }

    /**
     * 更新规则对应的warehouseAddresses
     *
     * @param ruleId
     * @param warehouseAddresses 仓库地址规则 列表
     * @return 对应的规则id
     */
    @Override
    public Response<Long> batchUpdate(Long ruleId, List<WarehouseAddress> warehouseAddresses) {
        return null;
    }

    @Override
    public Response<Boolean> update(WarehouseAddressRule warehouseAddressRule) {
        try {
            return Response.ok(warehouseAddressRuleDao.update(warehouseAddressRule));
        } catch (Exception e) {
            log.error("update warehouseAddressRule failed, warehouseAddressRule:{}, cause:{}", warehouseAddressRule, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.rule.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long warehouseAddressRuleId) {
        try {
            return Response.ok(warehouseAddressRuleDao.delete(warehouseAddressRuleId));
        } catch (Exception e) {
            log.error("delete warehouseAddressRule failed, warehouseAddressRuleId:{}, cause:{}", warehouseAddressRuleId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.rule.delete.fail");
        }
    }
}
