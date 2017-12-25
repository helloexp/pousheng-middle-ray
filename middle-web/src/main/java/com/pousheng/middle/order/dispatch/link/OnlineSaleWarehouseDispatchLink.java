package com.pousheng.middle.order.dispatch.link;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 电商仓发货规则
 * 优先级 2
 * 根据商品信息查找电商在售商品库存
 * 判断下单商品是否为电商在售的，过滤掉非mpos标签的仓，如果没有mpos下单可用的仓则进入下个规则，（mpos电商在售仓集合为空）
 * 如果查找到则判断是否可以整单发货，可以整单发货则直接发货。
 * 如果不能整单发货则记录到所匹配的电商在售仓集合中，供下个规则使用。
 * Created by songrenfei on 2017/12/23
 */
@Slf4j
public class OnlineSaleWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseSkuReadService warehouseSkuReadService;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        List<WarehouseSkuStock> allWarehouseSkuStock = Lists.newArrayList();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities){
            Response<List<WarehouseSkuStock>> warehouseSkuStockRes = warehouseSkuReadService.findBySkuCode(skuCodeAndQuantity.getSkuCode());
            if(!warehouseSkuStockRes.isSuccess()){
                log.error("failed to find stock for  skuCode={}, error:{}",
                        skuCodeAndQuantity.getSkuCode(), warehouseSkuStockRes.getError());
                continue;
            }
            allWarehouseSkuStock.addAll(warehouseSkuStockRes.getResult());
        }

        return false;
    }


    private List<WarehouseSkuStock> getOnlineSaleWarehouse(){

        return null;
    }
}
