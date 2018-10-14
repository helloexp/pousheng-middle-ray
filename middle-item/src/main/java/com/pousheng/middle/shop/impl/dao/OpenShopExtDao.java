package com.pousheng.middle.shop.impl.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.open.client.common.shop.model.OpenShop;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tanlongjun
 */
@Repository
public class OpenShopExtDao extends MyBatisDao<OpenShop> {

    public List<OpenShop> searchByOuterIdAndBusinessId(String outerId, String businessId) {
        return getSqlSession().selectList(sqlId("searchByOuterIdAndBusinessId"),
            ImmutableMap.of("outerId", outerId, "businessId", businessId));
    }

}
