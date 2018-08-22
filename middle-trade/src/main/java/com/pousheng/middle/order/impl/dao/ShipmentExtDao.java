package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.order.model.Shipment;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
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

}
