package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.model.SkuGroupRule;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@Repository
public class SkuGroupRuleDao extends MyBatisDao<SkuGroupRule> {

    public List<SkuGroupRule> findByCardId(String cardId){
        return getSqlSession().selectList(sqlId("findByCardId"), cardId);
    }
}
