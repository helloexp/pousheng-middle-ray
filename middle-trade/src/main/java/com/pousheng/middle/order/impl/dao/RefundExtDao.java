package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.RefundExt;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.order.model.Refund;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class RefundExtDao extends MyBatisDao<RefundExt> {

    public boolean updateTradeNo(Long id, String originTradeNo, String newTradeNo) {
        return getSqlSession().update(sqlId("updateTradeNo"), ImmutableMap.of("id", id, "originTradeNo", originTradeNo, "newTradeNo", newTradeNo)) == 1;
    }
    
	/**
	 * XXX RAY 2019.04.19 新增是否完善退物流，退回快遞單號、及退貨入庫時間進行篩選
	 * 
	 * @param pageInfo 分頁物件
	 * @param criteria 條件
	 * @return
	 */
	public Paging<Refund> pagingNew(PageInfo pageInfo, Map<String, Object> criteria) {
		if (criteria == null) {
			criteria = Maps.newHashMap();
		}

		Long total = (Long) sqlSession.selectOne(sqlId("countNew"), criteria);
		if (total.longValue() <= 0L) {
			return new Paging<Refund>(Long.valueOf(0L), Collections.emptyList());
		}
		criteria.put("offset", pageInfo.getOffset());
		criteria.put("limit", pageInfo.getLimit());

		List<Refund> datas = sqlSession.selectList(sqlId("pagingNew"), criteria);
		return new Paging<Refund>(total, datas);
	}
}
