package com.pousheng.middle.group.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.group.model.ItemRule;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Repository
public class ItemRuleDao extends MyBatisDao<ItemRule> {
    
    public Paging<ItemRule> pagingIds(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        Long total = (Long)getSqlSession().selectOne(this.sqlId("countIds"), criteria);
        if (total.longValue() <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            ((Map)criteria).put("offset", offset);
            ((Map)criteria).put("limit", limit);
            List<ItemRule> datas = getSqlSession().selectList(this.sqlId("pagingIds"), criteria);
            return new Paging(total, datas);
        }
    }

}
