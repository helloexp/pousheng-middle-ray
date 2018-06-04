package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: sku在仓库的库存情况Dao类
 * Date: 2017-06-07
 */
@Repository
public class WarehouseSkuStockDao extends MyBatisDao<WarehouseSkuStock> {

    /**
     * 分页查询
     *
     * @param offset   偏移
     * @param limit    每页返回条数
     * @param criteria 查询标准
     * @return 分页结果
     */
    public Paging<WarehouseSkuStock> pagingByDistinctSkuCode(Integer offset, Integer limit,
                                                             Map<String, Object> criteria) {
        Long total = this.sqlSession.selectOne(this.sqlId("countByDistinctSkuCode"), criteria);
        if (total <= 0L) {
            return new Paging<>(0L, Collections.<WarehouseSkuStock>emptyList());
        } else {
            criteria.put("offset", offset);
            criteria.put("limit", limit);
            List<WarehouseSkuStock> datas = this.sqlSession.selectList(this.sqlId("pagingByDistinctSkuCode"), criteria);
            return new Paging<>(total, datas);
        }
    }

    /**
     * 根据仓库id和sku编码查询对应的库存
     *
     * @param warehouseId 仓库id
     * @param skuCode sku编码
     * @return 对应的库存结果
     */
    public WarehouseSkuStock findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode) {
        return this.sqlSession.selectOne(sqlId("findByWarehouseIdAndSkuCode"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode));
    }

    /**
     * 根据sku编码查找在仓库中的总的可用库存
     *
     * @param skuCode sku编码
     * @return 库存结果
     */
    public WarehouseSkuStock findAvailStockBySkuCode(String skuCode){
        return this.getSqlSession().selectOne(sqlId("findAvailStockBySkuCode"), skuCode);
    }

    /**
     * 根据仓库id和sku编码查找对应的库存
     *
     * @param warehouseId 仓库id
     * @param skuCodes    sku编码
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
     * @param skuCode     sku代码
     * @param delta       变化的数值
     * @return 是否操作成功
     */
    public boolean decreaseStock(Long warehouseId, String skuCode, Integer delta) {
        return this.sqlSession.update(sqlId("decreaseStock"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode, "delta", delta))
                == 1;
    }

    /**
     * 锁库存, 减少可用库存, 增加锁定库存
     *
     * @param warehouseId 仓库id
     * @param skuCode     sku编码
     * @param delta       变化的数值
     * @return 是否操作成功
     */
    public boolean lockStock(Long warehouseId, String skuCode, Integer delta) {
        return this.sqlSession.update(sqlId("lockStock"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode, "delta", delta))
                == 1;
    }

    /**
     * 解锁库存
     *
     * @param warehouseId 仓库id
     * @param skuCode     sku编码
     * @param delta       变化的数值
     * @param currentLock  当前锁定库存数量
     * @return 是否操作成功
     */
    public boolean unlockStock(Long warehouseId, String skuCode, Integer delta,Long currentLock) {
        return this.sqlSession.update(sqlId("unlockStock"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode, "delta", delta,"currentLock",currentLock))
                == 1;
    }

    /**
     * 从erp同步库存, 只有大于上次同步请求的库存才进行处理
     *
     * @param warehouseId 仓库id
     * @param skuCode     sku编码
     * @param erpStock    erp的可用库存
     * @param updatedAt   库存的变化发生时间
     */
    public void syncStock(Long warehouseId, String skuCode, Integer erpStock, Date updatedAt) {
        WarehouseSkuStock exist = findByWarehouseIdAndSkuCode(warehouseId, skuCode);

        if(exist ==null){
            WarehouseSkuStock wss = new WarehouseSkuStock();
            wss.setAvailStock(erpStock.longValue());
            wss.setLockedStock(0L);
            wss.setBaseStock(erpStock.longValue());
            wss.setWarehouseId(warehouseId);
            wss.setSkuCode(skuCode);
            wss.setSyncAt(updatedAt);
            this.create(wss);
        }else {
            long id = exist.getId();
            this.sqlSession.update(sqlId("syncStock"),
                    ImmutableMap.of("id", id,
                            "erpStock", erpStock, "updatedAt", updatedAt));
        }
    }

    /**
     * 根据skuCode查询对应库存分布情况
     *
     * @param skuCode sku编码
     * @return 指定skuCode对应库存分布情况
     */
    public List<WarehouseSkuStock> findBySkuCode(String skuCode) {
        return this.sqlSession.selectList(sqlId("findBySkuCode"), skuCode);
    }
}
