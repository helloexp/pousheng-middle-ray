package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseSkuStockDao extends MyBatisDao<WarehouseSkuStock> {

    public Paging<WarehouseSkuStock> pagingByDistinctSkuCode(Integer offset, Integer limit,
                                                             Map<String, Object> criteria){
        Long total = this.sqlSession.selectOne(this.sqlId("countByDistinctSkuCode"), criteria);
        if(total <= 0L) {
            return new Paging<WarehouseSkuStock>(0L, Collections.<WarehouseSkuStock>emptyList());
        } else {
            criteria.put("offset", offset);
            criteria.put("limit", limit);
            List<WarehouseSkuStock> datas = this.sqlSession.selectList(this.sqlId("pagingByDistinctSkuCode"), criteria);
            return new Paging<WarehouseSkuStock>(total, datas);
        }
    }

    public WarehouseSkuStock findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode) {
        return  this.sqlSession.selectOne(sqlId("findByWarehouseIdAndSkuCode"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode));
    }
}
