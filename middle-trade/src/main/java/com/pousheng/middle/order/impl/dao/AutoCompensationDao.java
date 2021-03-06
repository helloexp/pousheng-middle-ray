package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.AutoCompensation;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by penghui on 2018/1/15
 */
@Repository
public class AutoCompensationDao extends MyBatisDao<AutoCompensation> {

    /**
     * 批量更新状态
     *
     * @param ids    id集合
     * @param status 状态
     */
    public void updateStatus(List<Long> ids, Integer status) {
        getSqlSession().update(sqlId("updateStatus"), ImmutableMap.of("ids", ids, "status", status));
    }

    /**
     * 查询指定状态的id集合
     *
     * @param ids    id集合
     * @param status 状态
     * @return
     */
    public List<AutoCompensation> findByIdsAndStatus(List<Long> ids, Integer status) {
        return getSqlSession().selectList(sqlId("findByIdsAndStatus"), ImmutableMap.of("ids", ids, "status", status));
    }

}
