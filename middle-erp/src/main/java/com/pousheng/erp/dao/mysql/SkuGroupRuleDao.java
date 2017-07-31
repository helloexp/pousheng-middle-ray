package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.erp.model.SkuGroupRule;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@Repository
public class SkuGroupRuleDao extends MyBatisDao<SkuGroupRule> {

    public List<SkuGroupRule> findByCardId(String cardId){
        return getSqlSession().selectList(sqlId("findByCardId"), cardId);
    }

    public SkuGroupRule findBYCardIdAndKindId(String cardId, String kindId){
        Map<String, String> params = Maps.newHashMap();
        params.put("cardId", cardId);
        if(StringUtils.hasText(kindId)){
            params.put("kindId",kindId);
        }
        return getSqlSession().selectOne(sqlId("findByCardIdAndKindId"),params);
    }
}
