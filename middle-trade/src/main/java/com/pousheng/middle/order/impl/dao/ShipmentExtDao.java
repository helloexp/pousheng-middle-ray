package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.order.model.Shipment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tanlongjun
 */
@Repository
public class ShipmentExtDao extends MyBatisDao<Shipment> {


    public Paging<Shipment> pagingExt(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }

        Long total = (Long)this.sqlSession.selectOne(this.sqlId("countExt"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            ((Map)criteria).put("offset", offset);
            ((Map)criteria).put("limit", limit);
            List<Shipment> datas = this.sqlSession.selectList(this.sqlId("pagingExt"), criteria);
            return new Paging(total, datas);
        }
    }

    /**
     * 能指定排序规则的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<Shipment> paging(Integer offset, Integer limit,String sort,Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        if(StringUtils.isNotBlank(sort)){
            criteria.put("sort",sort);
        }
        criteria.put("offset", offset);
        criteria.put("limit", limit);
        Long total = (Long)this.sqlSession.selectOne(this.sqlId("count"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            List<Shipment> datas = this.sqlSession.selectList(this.sqlId("pagingSort"), criteria);
            return new Paging(total, datas);
        }
    }
}
