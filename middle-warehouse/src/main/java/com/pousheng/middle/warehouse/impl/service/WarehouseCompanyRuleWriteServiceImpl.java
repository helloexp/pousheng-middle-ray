package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseCompanyRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库写服务实现类
 * Date: 2017-06-21
 */
@Slf4j
@Service
public class WarehouseCompanyRuleWriteServiceImpl implements WarehouseCompanyRuleWriteService {

    private final WarehouseCompanyRuleDao warehouseCompanyRuleDao;

    @Autowired
    public WarehouseCompanyRuleWriteServiceImpl(WarehouseCompanyRuleDao warehouseCompanyRuleDao) {
        this.warehouseCompanyRuleDao = warehouseCompanyRuleDao;
    }

    @Override
    public Response<Long> create(WarehouseCompanyRule warehouseCompanyRule) {
        try {
            String companyCode = warehouseCompanyRule.getCompanyCode();
            WarehouseCompanyRule exist = warehouseCompanyRuleDao.findByCompanyCode(companyCode);
            if(exist!=null){
                log.error("failed to create {}, because warehouse for company(code={}) has existed",
                        warehouseCompanyRule, companyCode);
                return Response.fail("company.code.duplicated");
            }
            warehouseCompanyRuleDao.create(warehouseCompanyRule);
            return Response.ok(warehouseCompanyRule.getId());
        } catch (Exception e) {
            log.error("create warehouseCompanyRule failed, warehouseCompanyRule:{}, cause:{}", warehouseCompanyRule, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(WarehouseCompanyRule warehouseCompanyRule) {
        try {
            return Response.ok(warehouseCompanyRuleDao.update(warehouseCompanyRule));
        } catch (Exception e) {
            log.error("update warehouseCompanyRule failed, warehouseCompanyRule:{}, cause:{}", warehouseCompanyRule, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long id) {
        try {
            return Response.ok(warehouseCompanyRuleDao.delete(id));
        } catch (Exception e) {
            log.error("delete warehouseCompanyRule failed, warehouseCompanyRuleId:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.delete.fail");
        }
    }
}
