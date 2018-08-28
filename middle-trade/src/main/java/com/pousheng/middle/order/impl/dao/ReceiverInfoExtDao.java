package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 收货人信息DAO扩展
 * @author tanlongjun
 */
@Repository
public class ReceiverInfoExtDao extends MyBatisDao<ReceiverInfo> {

    /**
     * 能指定排序规则的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<ReceiverInfo> paging(Integer offset, Integer limit,String sort,Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        if(StringUtils.isNotBlank(sort)){
            criteria.put("sort",sort);
        }
        criteria.put("offset", offset);
        criteria.put("limit", limit);
        Long total = (Long)this.sqlSession.selectOne(this.sqlId("countExt"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            List<Shipment> datas = this.sqlSession.selectList(this.sqlId("pagingSort"), criteria);
            return new Paging(total, datas);
        }
    }
}
