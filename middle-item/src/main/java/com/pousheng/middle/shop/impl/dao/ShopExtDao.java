package com.pousheng.middle.shop.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.shop.model.Shop;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class ShopExtDao extends MyBatisDao<Shop> {

    public Shop findByOuterIdAndBusinessId(String outerId,Long businessId){
        return getSqlSession().selectOne(sqlId("findByOuterIdAndBusinessId"), ImmutableMap.of("outerId",outerId,"businessId",businessId));
    }

	/**
	 * RAY: 分页查询门店信息
	 * 
	 * @param offset   頁次
	 * @param limit    筆數
	 * @param criteria SQL條件
	 * @return
	 */
	public Paging<Shop> pagingWithExpresssCompany(Integer offset, Integer limit, Map<String, Object> criteria) {
		if (criteria == null) {
			criteria = Maps.newHashMap();
		}

		Long total = (Long) sqlSession.selectOne(sqlId("countExp"), criteria);
		if (total.longValue() <= 0L) {
			return new Paging<Shop>(Long.valueOf(0L), Collections.emptyList());
		}
		criteria.put("offset", offset);
		criteria.put("limit", limit);
		List<Shop> data = getSqlSession().selectList(sqlId("pagingWithExpresssCompany"), criteria);
		return new Paging<Shop>(total, data);
	}
}
