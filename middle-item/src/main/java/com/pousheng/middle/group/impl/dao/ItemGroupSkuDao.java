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

    public Boolean deleteByGroupIdAndSkuCode(Long groupId, String skuCode) {
        return getSqlSession().delete(sqlId("deleteByGroupIdAndSkuCode"), ImmutableMap.of("groupId", groupId, "skuCode", skuCode)) > 0;
    }

    public List<ItemGroupSku> findBySkuCode(String skuCode) {
        return getSqlSession().selectList(sqlId("findBySkuCode"), skuCode);
    }

    public ItemGroupSku findByGroupIdAndSkuCode(Long groupId, String skuCode) {
        return getSqlSession().selectOne(sqlId("findByGroupIdAndSkuCode"), ImmutableMap.of("groupId", groupId, "skuCode", skuCode));
    }

    public Integer batchDelete(List<String> skuCodes, Long groupId, Integer type, Integer mark) {
        return getSqlSession().delete(sqlId("batchDelete"), ImmutableMap.of("groupId", groupId, "skuCodes", skuCodes, "type", type, "mark", mark));
    }

    public Long countGroupSku(Long groupId) {
        return getSqlSession().selectOne(sqlId("countGroupSku"), ImmutableMap.of("groupId", groupId, "type", PsItemGroupSkuType.GROUP.value()));
    }

}
