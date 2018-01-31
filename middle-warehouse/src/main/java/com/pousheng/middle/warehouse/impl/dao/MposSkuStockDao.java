package com.pousheng.middle.warehouse.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author: songrenfei
 * Desc: mpos下单sku锁定库存情况Dao类
 * Date: 2017-12-23
 */
@Repository
public class MposSkuStockDao extends MyBatisDao<MposSkuStock> {



    /**
     * 根据仓库id和sku编码查询对应的库存
     *
     * @param warehouseId 仓库id
     * @param skuCode sku编码
     * @return 对应的库存结果
     */
    public MposSkuStock findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode) {
        return this.sqlSession.selectOne(sqlId("findByWarehouseIdAndSkuCode"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode));
    }



    /**
     * 根据门店id和sku编码查询对应的库存
     *
     * @param shopId 门店id
     * @param skuCode sku编码
     * @return 对应的库存结果
     */
    public MposSkuStock findByShopIdAndSkuCode(Long shopId, String skuCode) {
        return this.sqlSession.selectOne(sqlId("findByShopIdAndSkuCode"),
                ImmutableMap.of("shopId", shopId, "skuCode", skuCode));
    }

    /**
     * 锁仓库库存
     *
     * @param warehouseId 仓库id
     * @param skuCode     sku编码
     * @param quantity    数量
     * @return 是否操作成功
     */
    public boolean lockStockWarehouse(Long warehouseId, String skuCode, Integer quantity) {
        return this.sqlSession.update(sqlId("lockStockWarehouse"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode, "quantity", quantity))
                == 1;
    }


    /**
     * 解锁仓库库存
     *
     * @param warehouseId 仓库id
     * @param skuCode     sku编码
     * @param quantity       数值
     * @return 是否操作成功
     */
    public boolean unlockStockWarehouse(Long warehouseId, String skuCode, Integer quantity) {
        return this.sqlSession.update(sqlId("unlockStockWarehouse"),
                ImmutableMap.of("warehouseId", warehouseId, "skuCode", skuCode, "quantity", quantity))
                == 1;
    }


    /**
     * 锁门店库存
     *
     * @param shopId 门店id
     * @param skuCode     sku编码
     * @param quantity    数量
     * @return 是否操作成功
     */
    public boolean lockStockShop(Long shopId, String skuCode, Integer quantity) {
        return this.sqlSession.update(sqlId("lockStockShop"),
                ImmutableMap.of("shopId", shopId, "skuCode", skuCode, "quantity", quantity))
                == 1;
    }

    /**
     * 解锁门店库存
     *
     * @param shopId 门店id
     * @param skuCode     sku编码
     * @param quantity      数值
     * @return 是否操作成功
     */
    public boolean unlockStockShop(Long shopId, String skuCode, Integer quantity) {
        return this.sqlSession.update(sqlId("unlockStockShop"),
                ImmutableMap.of("shopId", shopId, "skuCode", skuCode, "quantity", quantity))
                == 1;
    }


}
