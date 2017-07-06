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

    /**
     * 根据仓库id和sku编码查找对应的库存
     *
     * @param warehouseId 仓库id
     * @param skuCodes sku编码
     * @return 库存列表
     */
    public List<WarehouseSkuStock> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes) {
        return this.sqlSession.selectList(sqlId("findByWarehouseIdAndSkuCodes"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCodes", skuCodes));
    }

    /**
     * 减库存, 实际上是减掉锁定的库存
     *
     * @param warehouseId 仓库id
     * @param skuCode sku代码
     * @param delta  变化的数值
     * @return 是否操作成功
     */
    public boolean decreaseStock(Long warehouseId, String skuCode, Integer delta){
        return this.sqlSession.update(sqlId("decreaseStock"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode",skuCode, "delta",delta))
                == 1;
    }

    /**
     * 锁库存, 减少可用库存, 增加锁定库存
     *
     * @param warehouseId 仓库id
     * @param skuCode sku编码
     * @param delta 变化的数值
     * @return 是否操作成功
     */
    public boolean lockStock(Long warehouseId, String skuCode, Integer delta){
        return this.sqlSession.update(sqlId("lockStock"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode",skuCode, "delta",delta))
                == 1;
    }
}
