package com.pousheng.middle.category.impl.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.category.model.BackCategory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */
@Repository
public class PsBackCategoriesDao extends MyBatisDao<BackCategory> {

    public Paging<BackCategory> pagingBy(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        Long total = this.sqlSession.selectOne(this.sqlId("countByName"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            criteria.put("offset", offset);
            criteria.put("limit", limit);
            List<BackCategory> datas = this.sqlSession.selectList(this.sqlId("pagingName"), criteria);
            return new Paging(total, datas);
        }
    }


}
