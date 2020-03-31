package com.pousheng.middle.shop.impl.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.shop.model.Shop;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class ShopExtDao extends MyBatisDao<Shop> {

    public Shop findByOuterIdAndBusinessId(String outerId,Long businessId){
        return getSqlSession().selectOne(sqlId("findByOuterIdAndBusinessId"), ImmutableMap.of("outerId",outerId,"businessId",businessId));
    }

    public Shop findShopById(Long id){
        return getSqlSession().selectOne(sqlId("findShopById"), ImmutableMap.of("id",id));
    }

    public Shop findShopByUserName(String userName) {
        return getSqlSession().selectOne(sqlId("findShopByUserName"), ImmutableMap.of("userName", userName));
    }

    public List<Shop> findAllShopsOn(){
        return getSqlSession().selectList(sqlId("findAllShopsOn"));
    }

    public Shop findByUsername(String username) {
        return getSqlSession().selectOne(sqlId("findShopByUserName"), ImmutableMap.of("username",username));
    }

    public List<Shop> findByOuterIds(List<String> outerIds) {
        return getSqlSession().selectList(sqlId("findByOuterIds"), ImmutableMap.of("ids",outerIds));
    }
}
