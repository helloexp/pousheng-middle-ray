package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ShopOrderExt;
import com.pousheng.middle.order.model.SkuOrderExt;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Repository
public class ShopOrderExtDao extends MyBatisDao<ShopOrderExt> {
    public boolean updateBuyerNoteById(ShopOrderExt shopOrderExt){
        return getSqlSession().update(sqlId("updateBuyerNoteById"),shopOrderExt) == 1;
    }
}
