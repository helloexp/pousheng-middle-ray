package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 *
 * @author tony
 */
@Repository
public class PoushengCompensateBizDao extends MyBatisDao<PoushengCompensateBiz> {

    /**
     * 更新状态
     *
     * @param id            主键
     * @param currentStatus 当前状态
     * @param newStatus     需要更新的状态
     * @return
     */
    public boolean updateStatus(Long id, String currentStatus, String newStatus) {
        return getSqlSession().update(sqlId("updateStatus"), ImmutableMap.of("id", id, "currentStatus", currentStatus, "newStatus", newStatus)) == 1;
    }

    /**
     * 批量更新状态
     *
     * @param ids    id集合
     * @param status 状态
     */
    public void batchUpdateStatus(List<Long> ids, String status) {
        getSqlSession().update(sqlId("batchUpdateStatus"), ImmutableMap.of("ids", ids, "status", status));
    }

    /**
     * 重置状态
     */
    public void resetStatus() {
        getSqlSession().update(sqlId("resetStatus"));
    }

    /**
     * 查询指定状态的id集合
     *
     * @param ids    id集合
     * @param status 状态
     * @return
     */
    public List<PoushengCompensateBiz> findByIdsAndStatus(List<Long> ids, String status) {
        return getSqlSession().selectList(sqlId("findByIdsAndStatus"), ImmutableMap.of("ids", ids, "status", status));
    }
}
