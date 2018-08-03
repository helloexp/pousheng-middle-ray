package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private InventoryClient inventoryClient;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private WarehouseChooser warehouseChooser;
    @Autowired
    private DispatchComponent dispatchComponent;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;


    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-OnlineSaleWarehouseDispatchLink-2  order(id:{}) start...",shopOrder.getId());

        Warehouses4Address warehouses4Address = (Warehouses4Address)context.get(DispatchContants.WAREHOUSE_FOR_ADDRESS);

        //收货地址明细
        String address = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion() + receiverInfo.getDetail();
        String addressRegion = receiverInfo.getProvince() + receiverInfo.getCity() + receiverInfo.getRegion();
        context.put(DispatchContants.BUYER_ADDRESS,address);
        context.put(DispatchContants.BUYER_ADDRESS_REGION,addressRegion);

        //查找该批商品中电商在售的(要把mpos总店的商品排除掉)
        List<SkuCodeAndQuantity> onlineSaleSku = getOnlineSaleSku(skuCodeAndQuantities);
        if(CollectionUtils.isEmpty(onlineSaleSku)){
            return Boolean.TRUE;
        }
        List<AvailableInventoryDTO> onlineSaleWarehouses = getOnlineSaleWarehouseStock(onlineSaleSku, shopOrder);
        //如果不存在直接让下个规则处理
        if(CollectionUtils.isEmpty(onlineSaleWarehouses)){
            return Boolean.TRUE;
        }

        List<Long> warehouseIds = Lists.transform(onlineSaleWarehouses, input -> input.getWarehouseId());

        List<WarehouseDTO> warehouseList = findWarehouseByIds(warehouseIds);

        //过滤掉非mpos仓,过滤后如果没有则进入下个规则,过滤掉店仓直接用总仓
        List<WarehouseDTO> mposOnlineSaleWarehouse = warehouseList.stream().filter(warehouse ->
                Objects.equals(warehouse.getIsMpos(),1)).filter(warehouse ->
                Objects.equals(warehouse.getWarehouseSubType(),0)).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(mposOnlineSaleWarehouse)){
            return Boolean.TRUE;
        }

        //电商mpos 仓id集合
        List<Long> mposOnlineSaleWarehouseIds = Lists.transform(mposOnlineSaleWarehouse, new Function<WarehouseDTO, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable WarehouseDTO input) {
                return input.getId();
            }
        });

        //仓库 商品 库存
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = HashBasedTable.create();
        for (AvailableInventoryDTO warehouseSkuStock : onlineSaleWarehouses){

            //过滤掉非mpos的
            if(mposOnlineSaleWarehouseIds.contains(warehouseSkuStock.getWarehouseId())){
                //可用库存
                Integer availStock = warehouseSkuStock.getTotalAvailQuantity();
                log.info("[ONLINE-STOCK]-warehouse(id:{}) sku code:{} availStock:{}",warehouseSkuStock.getWarehouseId(),warehouseSkuStock.getSkuCode(),availStock);
                warehouseSkuCodeQuantityTable.put(warehouseSkuStock.getWarehouseId(),warehouseSkuStock.getSkuCode(),availStock);
            }
        }
        context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);

        //判断是否有整单
        List<WarehouseShipment> warehouseShipments = dispatchComponent.chooseSingleWarehouse(warehouseSkuCodeQuantityTable,skuCodeAndQuantities);
        //没有整单发的
        if(CollectionUtils.isEmpty(warehouseShipments)){
            return Boolean.TRUE;
        }

        //如果只有一个
        if(Objects.equals(warehouseShipments.size(),1)){
            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipments);
            return Boolean.FALSE;
        }

        //如果有多个要选择最近的
        WarehouseShipment warehouseShipment = warehouseAddressComponent.nearestWarehouse(warehouses4Address.getPriorityWarehouseIds(),warehouseShipments,address);
        dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList(warehouseShipment));

        return Boolean.FALSE;
    }


    //电商在售的仓
    private List<AvailableInventoryDTO> getOnlineSaleWarehouseStock(List<SkuCodeAndQuantity> skuCodeAndQuantities, ShopOrder shopOrder){
        Response<List<AvailableInventoryDTO>> skuStockInfos = inventoryClient.getAvailableInventory(
                dispatchComponent.getAvailInvReq(null, Lists.newArrayList(Lists.transform(skuCodeAndQuantities, input -> input.getSkuCode())))
                , shopOrder.getShopId());
        if(!skuStockInfos.isSuccess() || CollectionUtils.isEmpty(skuStockInfos.getResult())){
            log.warn("not skuStockInfos so skip");

            return Lists.newArrayList();
        }

        return skuStockInfos.getResult();
    }

    private List<WarehouseDTO> findWarehouseByIds(List<Long> ids){
        Response<List<WarehouseDTO>> warehouseListRes = warehouseClient.findByIds(ids);
        if(!warehouseListRes.isSuccess()){
            throw new ServiceException(warehouseListRes.getError());
        }
        return warehouseListRes.getResult();
    }

    private List<SkuCodeAndQuantity> getOnlineSaleSku(List<SkuCodeAndQuantity> skuCodeAndQuantities){
        List<SkuCodeAndQuantity> filterSkuCodeAndQuantitys =Lists.newArrayListWithCapacity(skuCodeAndQuantities.size());

        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities){
            Response<List<ItemMapping>> response = mappingReadService.findBySkuCode(skuCodeAndQuantity.getSkuCode());
            if(!response.isSuccess()){
                log.error("find item mapping by sku code:{} fail,error:{}",skuCodeAndQuantity.getSkuCode(),response.getError());
                throw new ServiceException(response.getError());
            }
            List<ItemMapping> itemMappingList = response.getResult();

            List<ItemMapping> onlineItemMappingList = itemMappingList.stream().filter(itemMapping -> !Objects.equals(itemMapping.getOpenShopId(),mposOpenShopId)).collect(Collectors.toList());
            //不为空则说明电商在售
            if(!CollectionUtils.isEmpty(onlineItemMappingList)){
                filterSkuCodeAndQuantitys.add(skuCodeAndQuantity);
            }
        }
        return filterSkuCodeAndQuantitys;
    }
}
