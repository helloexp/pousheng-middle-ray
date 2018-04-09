package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.exception.ServiceException;
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
 * 省内仓发货规则
 * 优先级 3
 * 1、查找用户收货地址所在的省
 * 2、查找省内的打了mpos标签的仓，如果没有找到则进入下个规则
 * 3、根据商品信息和仓的范围（已过滤掉电商在售仓，过滤后如果没有可用的范围则进入下个规则）调用roger的接口查询各个仓及仓下对应商品的库存
 * 4、判断是否有整单发货的仓，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该仓发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个仓
 * Created by songrenfei on 2017/12/22
 */
@Component
@Slf4j
public class ProvinceInnerWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private DispatchComponent dispatchComponent;


    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-ProvinceInnerWarehouseDispatchLink-3  order(id:{}) start...",shopOrder.getId());

        //从上个规则传递过来。
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(warehouseSkuCodeQuantityTable)){
            warehouseSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);
        }
        Set<Long>  alreadyQueryWarehouseIds = warehouseSkuCodeQuantityTable.rowKeySet();

        //省内的mpos仓,如果没有则进入下个规则
        List<AddressGps> addressGpses = warehouseAddressComponent.findWarehouseAddressGps(Long.valueOf(receiverInfo.getProvinceId()));
        if(CollectionUtils.isEmpty(addressGpses)){
            return Boolean.TRUE;
        }

        //过滤掉上个规则匹配到的电商在售仓,过滤后如果没有可用的范围则进入下个规则
        List<AddressGps> rangeInnerAddressGps = addressGpses.stream().filter(addressGps -> !alreadyQueryWarehouseIds.contains(addressGps.getBusinessId())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(rangeInnerAddressGps)){
            return Boolean.TRUE;
        }

        List<Long> warehouseIds = Lists.transform(rangeInnerAddressGps, new Function<AddressGps, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable AddressGps input) {
                return input.getBusinessId();
            }
        });

        List<Warehouse> warehouses = warehouseAddressComponent.findWarehouseByIds(warehouseIds);

        //过滤掉非mpos仓
        List<Warehouse> mposWarehouses = warehouses.stream().filter(warehouse -> Objects.equal(warehouse.getIsMpos(),1)).filter(warehouse -> java.util.Objects.equals(warehouse.getType(),0)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(mposWarehouses)){
            return Boolean.TRUE;
        }

        //查询仓代码
        List<String> stockCodes = dispatchComponent.getWarehouseOutCode(mposWarehouses);

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);


        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodes,skuCodes,2);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            return Boolean.TRUE;
        }

        // 放入 warehouseSkuCodeQuantityTable
        dispatchComponent.completeWarehouseTab(skuStockInfos,warehouseSkuCodeQuantityTable);

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


}
