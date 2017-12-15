package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表Dao类
 * Date: 2017-12-15
 */
@Repository
public class AddressGpsDao extends MyBatisDao<AddressGps> {

    public AddressGps findByBusinessIdAndType(Long businessId,AddressBusinessType type){
        return getSqlSession().selectOne(sqlId("findByBusinessIdAndType"), ImmutableMap.of("businessId",businessId,"businessType",type.getValue()));
    }

    public List<AddressGps> findByProvinceIdAndBusinessType(Long provinceId, AddressBusinessType type){
        return getSqlSession().selectList(sqlId("findByProvinceIdType"), ImmutableMap.of("provinceId",provinceId,"businessType",type.getValue()));
    }

    public List<AddressGps> findByRegionIdAndBusinessType(Long regionId,AddressBusinessType type){
        return getSqlSession().selectList(sqlId("findByRegionIdType"), ImmutableMap.of("regionId",regionId,"businessType",type.getValue()));
    }

}
