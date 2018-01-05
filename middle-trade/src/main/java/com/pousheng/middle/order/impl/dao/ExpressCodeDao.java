package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ExpressCode;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tony on 2017/6/27.
 */
@Repository
public class ExpressCodeDao extends MyBatisDao<ExpressCode> {

    public ExpressCode findByName(String expressName) {
        return getSqlSession().selectOne(sqlId("findByName"), expressName);
    }
    public ExpressCode findByOfficalCode(String officialCode) {
        return getSqlSession().selectOne(sqlId("findByOfficalCode"), officialCode);
    }

    public List<ExpressCode> findAllByName(String name) {
        return getSqlSession().selectList(sqlId("findAll"),name);
    }
}
