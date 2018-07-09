package com.pousheng.middle.group.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.group.model.ItemGroupSku;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Repository
public class ItemGroupSkuDao extends MyBatisDao<ItemGroupSku> {

    public Boolean deleteByGroupIdAndSkuId(Long groupId, Long skuId) {
        return getSqlSession().delete(sqlId("deleteByGroupIdAndSkuId"), ImmutableMap.of("groupId", groupId, "skuId", skuId)) > 0;
    }

    public List<ItemGroupSku> findBySkuId(Long skuId) {
        return getSqlSession().selectList(sqlId("findBySkuId"), skuId);
    }

    public ItemGroupSku findByGroupIdAndSkuId(Long groupId, Long skuId) {
        return getSqlSession().selectOne(sqlId("findByGroupIdAndSkuId"), ImmutableMap.of("groupId", groupId, "skuId", skuId));
    }

    public Integer batchDelete(List<Long> skuIds, Long groupId, Integer type) {
        return getSqlSession().delete(sqlId("batchDelete"), ImmutableMap.of("groupId", groupId, "skuIds", skuIds, "type", type));
    }

    public Long countGroupSku(Long groupId) {
        return getSqlSession().selectOne(sqlId("countGroupSku"), ImmutableMap.of("groupId", groupId, "type", PsItemGroupSkuType.GROUP.value()));
    }

}
