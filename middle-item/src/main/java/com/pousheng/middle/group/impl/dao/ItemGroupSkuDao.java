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

    public Integer batchDeleteByIds(List<Long> ids) {
        return getSqlSession().delete(sqlId("batchDeleteByIds"), ImmutableMap.of("ids", ids));
    }

    public Long countGroupSku(Long groupId) {
        return getSqlSession().selectOne(sqlId("countGroupSku"), ImmutableMap.of("groupId", groupId, "type", PsItemGroupSkuType.GROUP.value()));
    }

    public Long countGroupSkuAndType(Long groupId,Integer type) {
        return getSqlSession().selectOne(sqlId("countGroupSku"), ImmutableMap.of("groupId", groupId, "type", type));
    }

    public List<ItemGroupSku> findByGroupId(Long groupId) {
        return getSqlSession().selectList(sqlId("findByGroupId"), groupId);
    }

    public List<ItemGroupSku> findByGroupIdAndType(Long groupId, Integer type) {
        return getSqlSession().selectList(sqlId("findByGroupIdAndType"), ImmutableMap.of("groupId",groupId,"type",type));
    }
    
    public List<Long> findGroupIdsBySkuCodeAndType(List<String> skuCodes, Integer type){
        return getSqlSession().selectList(sqlId("findGroupIdsBySkuCodeAndType"), ImmutableMap.of("skuCodes",skuCodes,"type",type));
    }

}
