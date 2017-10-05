package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.MiddleShopOrder;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/5
 * pousheng-middle
 */
@Repository
public class MiddleShopOrderDao extends MyBatisDao<MiddleShopOrder> {
   public Paging<MiddleShopOrder> pagingAlias(Integer offset, Integer limit, Map<String, Object> criteria){
       if (criteria == null) {    //如果查询条件为空
           criteria = Maps.newHashMap();
       }
       // get total count
       Long total = getSqlSession().selectOne(sqlId("count"), criteria);
       if (total <= 0){
           return new Paging<MiddleShopOrder>(0L, Collections.<MiddleShopOrder>emptyList());
       }
       criteria.put(Constants.VAR_OFFSET, offset);
       criteria.put(Constants.VAR_LIMIT, limit);
       // get data
       List<MiddleShopOrder> datas = getSqlSession().selectList(sqlId("paging"), criteria);
       return new Paging<MiddleShopOrder>(total, datas);
   }
}
