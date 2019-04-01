package com.pousheng.middle.warehouse.impl.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.open.client.common.shop.model.OpenShop;

/**
 * @author jacky.chang
 * @date 2019/03/29
 */


@Repository
public class OpenShopWarehouseExtDao extends MyBatisDao<OpenShop>{

    public Paging<OpenShop> pagingWithConditions(Integer offset, Integer limit, Map<String, Object> warehouseExtra){
		if (warehouseExtra == null) {
			warehouseExtra = Maps.newHashMap();
		}
  
		Long total = (Long)sqlSession.selectOne(sqlId("countWithConditions"), warehouseExtra);
		if (total.longValue() <= 0L) {
			return new Paging<OpenShop>(Long.valueOf(0L), Collections.emptyList());
		}
		warehouseExtra.put("offset", offset);
		warehouseExtra.put("limit", limit);
		
		List<OpenShop> datas = sqlSession.selectList(sqlId("pagingWithConditions"), warehouseExtra);
		return new Paging<OpenShop>(total, datas);
	}
}
