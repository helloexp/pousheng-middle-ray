package com.pousheng.middle.order.impl.dao;


import com.pousheng.middle.order.model.SkuOrderExt;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/2.
 * pousheng-middle
 */
@Repository
public class SkuOrderExtDao extends MyBatisDao<SkuOrderExt> {

    public boolean updateSkuCodeAndSkuIdById(SkuOrderExt skuOrderExt){
        return getSqlSession().update(sqlId("updateSkuCodeAndSkuIdById"),skuOrderExt) == 1;
    }
}
