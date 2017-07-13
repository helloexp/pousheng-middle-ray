package com.pousheng.middle.warehouse.impl.dao;

import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库Dao类
 * Date: 2017-06-21
 */
@Repository
public class WarehouseCompanyRuleDao extends MyBatisDao<WarehouseCompanyRule> {

    /**
     * 根据公司编码查找对应的公司规则
     *
     * @param companyCode 公司编码
     * @return 对应的公司规则
     */
    public WarehouseCompanyRule findByCompanyCode(String companyCode) {
        return getSqlSession().selectOne(sqlId("findByCompanyCode"), companyCode);
    }

    /**
     * 获取公司编码列表
     *
     * @return 公司编码列表
     */
    public List<String>  companyCodes(){
        return getSqlSession().selectList(sqlId("findCompanyCodes"));
    }
}
