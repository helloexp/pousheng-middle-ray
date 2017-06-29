package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ExpressCode;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/6/27.
 */
@Repository
public class ExpressCodeDao extends MyBatisDao<ExpressCode> {

    public ExpressCode findByExpressName(String expressName) {
        return getSqlSession().selectOne(sqlId("findByExpressName"), expressName);
    }

}
