package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全国仓发货规则
 * 优先级 4
 * 1、调用roger的接口查询全国仓及仓下对应商品的库存
 * 2、过滤掉电商在售仓和省内仓，过滤后如果没有可用的范围则进入下个规则
 * 3、判断是否有整单发货的仓，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该仓发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个仓
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class AllWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-AllWarehouseDispatchLink-4  order(id:{}) start...",shopOrder.getId());



        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(warehouseSkuCodeQuantityTable)){
            warehouseSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);
        }
        List<String> skuCodes = Lists.transform(skuCodeAndQuantities, new Function<SkuCodeAndQuantity, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuCodeAndQuantity input) {
                return input.getSkuCode();
            }
        });

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(null,skuCodes,2);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            return Boolean.TRUE;
        }

        List<HkSkuStockInfo> filterSkuStockInfos = filterAlreadyQueryWarehouseSkuCodeQuantityTable(skuStockInfos,warehouseSkuCodeQuantityTable);
        if(CollectionUtils.isEmpty(filterSkuStockInfos)){
            return Boolean.TRUE;
        }

        List<Long> warehouseIds = Lists.transform(filterSkuStockInfos, new Function<HkSkuStockInfo, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable HkSkuStockInfo input) {
                return input.getBusinessId();
            }
        });

        List<Warehouse> warehouses = warehouseAddressComponent.findWarehouseByIds(warehouseIds);

        //过滤掉非mpos仓
        List<Warehouse> isMposWarehouses = warehouses.stream().filter(warehouse -> Objects.equal(warehouse.getIsMpos(),1)).filter(warehouse -> java.util.Objects.equals(warehouse.getType(),0)).collect(Collectors.toList());
        //没有有效的则跳过
        if(CollectionUtils.isEmpty(isMposWarehouses)){
            return Boolean.TRUE;
        }

        List<Long> isMposIds = Lists.transform(isMposWarehouses, new Function<Warehouse, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Warehouse input) {
                return input.getId();
            }
        });

        //有效
        List<HkSkuStockInfo> validSkuStockInfos = filterSkuStockInfos.stream().filter(hkSkuStockInfo -> isMposIds.contains(hkSkuStockInfo.getBusinessId())).collect(Collectors.toList());


        // 放入 warehouseSkuCodeQuantityTable
        dispatchComponent.completeWarehouseTab(validSkuStockInfos,warehouseSkuCodeQuantityTable);

        //判断是否有整单
        List<WarehouseShipment> warehouseShipments = dispatchComponent.chooseSingleWarehouse(warehouseSkuCodeQuantityTable,skuCodeAndQuantities);

        //没有整单发的
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Boolean.TRUE;
        }

        //如果只有一个
        if(Objects.equal(warehouseShipments.size(),1)){
            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
            return Boolean.FALSE;
        }

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
        //如果有多个要选择最近的
        WarehouseShipment warehouseShipment = warehouseAddressComponent.nearestWarehouse(warehouseShipments,address);
        dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));

        return Boolean.FALSE;
    }


    //过滤掉省内和电商在售仓
    private List<HkSkuStockInfo> filterAlreadyQueryWarehouseSkuCodeQuantityTable(List<HkSkuStockInfo> hkSkuStockInfos,Table<Long, String, Integer> warehouseSkuCodeQuantityTable){
       Set<Long> alreadyQueryWarehouseIds =  warehouseSkuCodeQuantityTable.rowKeySet();
        return hkSkuStockInfos.stream().filter(hkSkuStockInfo -> !alreadyQueryWarehouseIds.contains(hkSkuStockInfo.getBusinessId())).collect(Collectors.toList());

    }



}
