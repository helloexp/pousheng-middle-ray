package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockReadService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
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
@Component
@Slf4j
public class OnlineSaleWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private WarehouseChooser warehouseChooser;
    @Autowired
    private DispatchComponent dispatchComponent;


    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {


        //收货地址明细
        String address = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion() + receiverInfo.getDetail();
        context.put(DispatchContants.BUYER_ADDRESS,address);

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


        //电商mpos 仓id集合
        List<Long> mposOnlineSaleWarehouseIds = Lists.transform(mposOnlineSaleWarehouse, new Function<Warehouse, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Warehouse input) {
                return input.getId();
            }
        });

        dispatchOrderItemInfo.setMposOnlineSaleWarehouseIds(mposOnlineSaleWarehouseIds);


        //仓库 商品 库存
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = HashBasedTable.create();
        for (WarehouseSkuStock warehouseSkuStock : onlineSaleWarehouses){
            //过滤掉非mpos的
            if(mposOnlineSaleWarehouseIds.contains(warehouseSkuStock.getWarehouseId())){
                //可用库存要减去mpos占用部分
                Long availStock = warehouseSkuStock.getAvailStock();
                availStock-=dispatchComponent.getMposSkuWarehouseLockStock(warehouseSkuStock.getWarehouseId(),warehouseSkuStock.getSkuCode());
                warehouseSkuCodeQuantityTable.put(warehouseSkuStock.getWarehouseId(),warehouseSkuStock.getSkuCode(),Integer.valueOf(availStock.toString()));
            }
        }
        context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);

        //判断是否可以整单发货
        List<WarehouseShipment> warehouseShipments = warehouseChooser.chooseMposOnlineSaleSingleWarehouse(mposOnlineSaleWarehouse,warehouseSkuCodeQuantityTable,skuCodeAndQuantities);
        //没有整单发的
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Boolean.TRUE;
        }

        //如果只有一个
        if(Objects.equal(warehouseShipments.size(),1)){
            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
            return Boolean.FALSE;
        }

        //如果有多个要选择最近的
        WarehouseShipment warehouseShipment = warehouseAddressComponent.nearestWarehouse(warehouseShipments,address);
        dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));

        return Boolean.FALSE;
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
