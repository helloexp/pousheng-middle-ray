package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        List<WarehouseSkuStock> onlineSaleWarehouses = getOnlineSaleWarehouse(skuCodeAndQuantities);
        //如果不存在直接让下个规则处理
        if(CollectionUtils.isEmpty(onlineSaleWarehouses)){
            return Boolean.TRUE;
        }

        List<Long> warehouseIds = Lists.transform(onlineSaleWarehouses, new Function<WarehouseSkuStock, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable WarehouseSkuStock input) {
                return input.getWarehouseId();
            }
        });

        List<Warehouse> warehouseList = findWarehouseByIds(warehouseIds);

        //过滤掉非mpos仓,过滤后如果没有则进入下个规则
        List<Warehouse> mposOnlineSaleWarehouse = warehouseList.stream().filter(warehouse -> Objects.equal(warehouse.getIsMpos(),1)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(mposOnlineSaleWarehouse)){
            return Boolean.TRUE;
        }

        //判断是否可以整单发货


        context.put("mposOnlineSaleWarehouse", (Serializable) warehouseList);

        return false;
    }


    //电商在售的仓
    private List<WarehouseSkuStock> getOnlineSaleWarehouse(List<SkuCodeAndQuantity> skuCodeAndQuantities){
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
        return allWarehouseSkuStock;
    }

    private List<Warehouse> findWarehouseByIds(List<Long> ids){
        Response<List<Warehouse>> warehouseListRes = warehouseReadService.findByIds(ids);
        if(!warehouseListRes.isSuccess()){
            throw new ServiceException(warehouseListRes.getError());
        }
        return warehouseListRes.getResult();
    }
}
