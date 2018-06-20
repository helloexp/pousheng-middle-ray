package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.ShopAddressComponent;
import com.pousheng.middle.order.dispatch.component.WarehouseAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import com.pousheng.middle.warehouse.model.Warehouse;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 店仓发货规则
 * 优先级 3
 * 判断上个规则传过来的仓信息中是否有店仓，无则进入下个规则
 * 判过滤掉已拒绝过的门店，如果过滤后没有可用的门店则进入下个规则
 * 过滤掉已删除或已冻结的门店，如果过滤后没有可用的门店则进入下个规则
 * 如果查找到则判断是否可以整单发货，可以整单发货则直接发货。
 * 如果不能整单发货则记录到所匹配的电商在售仓集合中，供下个规则使用。
 * Created by songrenfei on 2017/12/23
 */
@Component
@Slf4j
public class ShopWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private WarehouseAddressComponent warehouseAddressComponent;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private ShopAddressComponent shopAddressComponent;
    @Autowired
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    private static final Ordering<WarehouseWithPriority> byPriority = Ordering.natural().onResultOf(new Function<WarehouseWithPriority, Integer>() {
        @Override
        public Integer apply(WarehouseWithPriority input) {
            return input.getPriority();
        }
    });


    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-ShopWarehouseDispatchLink-3  order(id:{}) start...",shopOrder.getId());

        Warehouses4Address warehouses4Address = (Warehouses4Address)context.get(DispatchContants.WAREHOUSE_FOR_ADDRESS);
        List<WarehouseWithPriority> shopWarehouseWithPriorities = warehouses4Address.getShopWarehouses();
        //判断上个规则传过来的仓信息中是否有店仓，无则进入下个规则
        if(CollectionUtils.isEmpty(shopWarehouseWithPriorities)){
            log.warn("not shopWarehouseWithPriorities so skip");
            return Boolean.TRUE;
        }

        List<Shop> shops = Lists.newArrayListWithCapacity(shopWarehouseWithPriorities.size());
        List<Long> shopWarehouseIds =Lists.newArrayListWithCapacity(shopWarehouseWithPriorities.size());
        //封装店铺id
        shopWarehouseWithPriorities.forEach(shopWarehouseWithPrioritie->{
            Warehouse warehouse = warehouseCacher.findById(shopWarehouseWithPrioritie.getWarehouseId());
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(),Long.valueOf(warehouse.getCompanyId()));
            shops.add(shop);
            shopWarehouseWithPrioritie.setShopId(shop.getId());
            shopWarehouseIds.add(warehouse.getId());

        });

        //拒绝过的门店
        List<Long> rejectShopIds = dispatchComponent.findRejectedShop(shopOrder.getId());
        context.put(DispatchContants.REJECT_SHOP_IDS, (Serializable) rejectShopIds);


        //过滤掉已删除或已冻结或已拒绝过的门店
        List<Shop> validShops = shops.stream().filter(shop -> com.google.common.base.Objects.equal(shop.getStatus(),1)&&!rejectShopIds.contains(shop.getId())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(validShops)){
            log.warn("not validShops so skip");
            return Boolean.TRUE;
        }
        //查询仓代码
        List<String> stockCodes = Lists.transform(validShops, new Function<Shop, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Shop input) {
                if(Strings.isNullOrEmpty(input.getOuterId())){
                    log.error("shop(id:{}) outer id invalid",input.getId());
                    throw new ServiceException("shop.outer.id.invalid");
                }
                return input.getOuterId();
            }
        });

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(shopWarehouseIds,skuCodes);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            log.warn("not skuStockInfos so skip");
            return Boolean.TRUE;
        }
        Table<Long, String, Integer> shopSkuCodeQuantityTable = HashBasedTable.create();
        dispatchComponent.completeShopTab(skuStockInfos,shopSkuCodeQuantityTable);
        context.put(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE, (Serializable) shopSkuCodeQuantityTable);
        //判断是否有整单
        List<ShopShipment> shopShipments = dispatchComponent.chooseSingleShop(shopSkuCodeQuantityTable,skuCodeAndQuantities);

        //没有整单发的
        if(CollectionUtils.isEmpty(shopShipments)){
            log.warn("not shopShipments so skip");
            return Boolean.TRUE;
        }

        //如果只有一个
        if(com.google.common.base.Objects.equal(shopShipments.size(),1)){
            dispatchOrderItemInfo.setShopShipments(shopShipments);
            return Boolean.FALSE;
        }


        //优先级类型
        WarehouseRuleItemPriorityType priorityType = WarehouseRuleItemPriorityType.from(warehouses4Address.getWarehouseRule().getItemPriorityType());

        ShopShipment shopShipment = null;
        switch (priorityType){
            case DISTANCE:
                String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
                String addressRegion = (String) context.get(DispatchContants.BUYER_ADDRESS_REGION);
                //如果有多个要选择最近的
                shopShipment = shopAddressComponent.nearestShop(shopShipments,address,addressRegion);
                break;
            case PRIORITY:
                //如果有多个要选择优先级最高的
                shopShipment = warehouseAddressComponent.priorityShop(byPriority.sortedCopy(shopWarehouseWithPriorities),shopShipments);
                break;
        }

        dispatchOrderItemInfo.setShopShipments(Lists.newArrayList(shopShipment));

        return Boolean.FALSE;
    }

}
