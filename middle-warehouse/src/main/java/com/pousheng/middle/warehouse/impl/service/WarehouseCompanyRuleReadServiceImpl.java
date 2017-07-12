package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseCompanyRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 店铺的公司规则库读服务实现类
 * Date: 2017-06-21
 */
@Slf4j
@Service
public class WarehouseCompanyRuleReadServiceImpl implements WarehouseCompanyRuleReadService {

    private final WarehouseCompanyRuleDao warehouseCompanyRuleDao;

    @Autowired
    public WarehouseCompanyRuleReadServiceImpl(WarehouseCompanyRuleDao warehouseCompanyRuleDao) {
        this.warehouseCompanyRuleDao = warehouseCompanyRuleDao;
    }

    @Override
    public Response<WarehouseCompanyRule> findById(Long id) {
        try {
            return Response.ok(warehouseCompanyRuleDao.findById(id));
        } catch (Exception e) {
            log.error("find warehouseCompanyRule by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.find.fail");
        }
    }

    /**
     * 根据公司编码查找对应的公司规则
     *
     * @param companyCode 公司编码
     * @return 公司对应的规则
     */
    @Override
    public Response<WarehouseCompanyRule> findByCompanyCode(String companyCode) {
        try {
            return Response.ok(warehouseCompanyRuleDao.findByCompanyCode(companyCode));
        } catch (Exception e) {
            log.error("find WarehouseCompanyRule by companyCode :{} failed,  cause:{}",
                    companyCode, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.code.find.fail");
        }
    }

    /**
     * 分页查找公司规则库
     *
     * @param pageNo   起始页码
     * @param pageSize 每页显示条数
     * @param params   查询参数
     * @return 店铺的公司规则库列表
     */
    @Override
    public Response<Paging<WarehouseCompanyRule>> pagination(Integer pageNo,
                                                             Integer pageSize,
                                                             Map<String, Object> params) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<WarehouseCompanyRule> p = warehouseCompanyRuleDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        } catch (Exception e) {
            log.error("failed to pagination WarehouseCompanyRules, params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.find.fail");
        }
    }

    /**
     * 获取所有的已设置规则的公司的编码
     *
     * @return 已设置规则的公司的编码
     */
    @Override
    public Response<List<String>> findCompanyCodes() {

        try {
            return Response.ok(warehouseCompanyRuleDao.companyCodes());
        } catch (Exception e) {
            log.error("failed to find company codes for company rules, error:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.company.rule.find.fail");
        }
    }
}
