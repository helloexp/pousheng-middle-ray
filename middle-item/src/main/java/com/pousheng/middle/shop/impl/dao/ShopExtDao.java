package com.pousheng.middle.shop.impl.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.shop.model.Shop;
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

}
