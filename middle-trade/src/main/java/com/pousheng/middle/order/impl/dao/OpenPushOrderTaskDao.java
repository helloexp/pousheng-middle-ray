package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.OpenPushOrderTask;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 * @author tony
 */
@Repository
public class OpenPushOrderTaskDao extends MyBatisDao<OpenPushOrderTask>{
    public List<OpenPushOrderTask> findByStatus(int status) {
        return getSqlSession().selectList(sqlId("findByStatus"), status);
    }
}
