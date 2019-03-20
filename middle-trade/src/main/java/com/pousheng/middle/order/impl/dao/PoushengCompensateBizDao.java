package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * 
     * @param context
     * @param currentStatus
     * @param newStatus
     * @return
     */
    public boolean updateStatusByContext(String context, String currentStatus, String newStatus, String bizType) {
        return getSqlSession().update(sqlId("updateStatusByContextInTwoHours"),
                ImmutableMap.of("context", context, "currentStatus", currentStatus, "newStatus", newStatus, "bizType",
                        bizType)) == 1;
    }

    /**
     * 更新type
     * 
     * @param id
     * @param currentType
     * @param newStatus
     * @return
     */
    public boolean updateTypeByContextOnlyIfOfWaitHandleStatus(String context, String currentType, String newType) {
        return getSqlSession().update(sqlId("updateTypeByContextOnlyIfOfWaitHandleStatus"),
                ImmutableMap.of("context", context, "currentType", currentType, "newType", newType)) == 1;
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

    public Paging<PoushengCompensateBiz> pagingForShow(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        Long total = (Long)this.sqlSession.selectOne(this.sqlId("count"), criteria);
        if (total.longValue() <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            ((Map)criteria).put("offset", offset);
            ((Map)criteria).put("limit", limit);
            List<PoushengCompensateBiz> datas = this.sqlSession.selectList(this.sqlId("pagingForShow"), criteria);
            return new Paging(total, datas);
        }
    }

    public Paging<Long> pagingIds(Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }

        Long total = (Long)this.sqlSession.selectOne(this.sqlId("count"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            List<Long> datas = getSqlSession().selectList(this.sqlId("pagingIds"), criteria);
            return new Paging(total, datas);
        }
    }
}
