package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.SkuOrderExt;
import com.pousheng.middle.order.model.SkuOrderLockStock;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tony on 2017/8/2.
 * pousheng-middle
 */
@Repository
public class SkuOrderExtDao extends MyBatisDao<SkuOrderExt> {

    public boolean updateSkuInfoById(SkuOrderExt skuOrderExt){
        return getSqlSession().update(sqlId("updateSkuInfoById"),skuOrderExt) == 1;
    }

    public void updateBuyerNameByOrderId(Long orderId, String buyerName) {
        getSqlSession().update(sqlId("updateBuyerNameByOrderId"), ImmutableMap.of("orderId", orderId, "buyerName", buyerName));
    }

    /**
     * 查询sku_order 中占用的数量
     * @param shopIds
     * @param warehouseIds
     * @param skuCodes
     * @param type
     * @return
     */
    public List<SkuOrderLockStock> queryOccupyQuantityList(List<Long> shopIds,
                                                           List<Long> warehouseIds, List<String> skuCodes,Integer type){
        return  getSqlSession().selectList(sqlId("findOccupyGroup"),
            ImmutableMap.of("shopIds", shopIds,
                "warehouseIds", warehouseIds,
                "skuCodes",skuCodes,
                "type",type));

    }

    /**
     * 批量查询sku订单列表
     * @param orderIds
     * @return
     */
    public List<String> findSkuCodesByOrderIds(List<Long> orderIds){
        return  getSqlSession().selectList(sqlId("findSkuCodesByOrderIds"),
            ImmutableMap.of("orderIds",orderIds));
    }
    
	/**
	 * XXX RAY 2019.04.25: 用訂單ID和第三方傳入的SKU代碼，查詢
	 * 
	 * @param orderId       訂單代碼
	 * @param originSkuCode 第三方傳入的SKU代碼
	 * @return
	 */
	public List<SkuOrder> findSkuOrderByOrderIdAndOriginSkuCode(String orderId, String originSkuCode) {
		return getSqlSession().selectList(sqlId("findSkuCodesByOrderIdAndOriginSkuCode"),
				ImmutableMap.of("orderId", orderId, "originSkuCode", originSkuCode));
	}
}
