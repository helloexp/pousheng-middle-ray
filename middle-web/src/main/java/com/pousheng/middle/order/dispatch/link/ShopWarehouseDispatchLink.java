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
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

        String companyCode = (String) context.get(DispatchContants.COMPANY_ID);
        Boolean oneCompany = (Boolean) context.get(DispatchContants.ONE_COMPANY);
        Warehouses4Address warehouses4Address = (Warehouses4Address)context.get(DispatchContants.WAREHOUSE_FOR_ADDRESS);
        List<WarehouseWithPriority> shopWarehouseWithPriorities = warehouses4Address.getShopWarehouses();

        //如果要求同公司则过滤掉其他公司的仓库，反之过滤掉同公司的
        if (oneCompany) {
            shopWarehouseWithPriorities = shopWarehouseWithPriorities.stream().filter(shop -> warehouseCacher.findById(shop.getWarehouseId()).getCompanyId().equals(companyCode)).collect(Collectors.toList());
        } else {
            shopWarehouseWithPriorities = shopWarehouseWithPriorities.stream().filter(shop -> !warehouseCacher.findById(shop.getWarehouseId()).getCompanyId().equals(companyCode)).collect(Collectors.toList());
        }

        //判断上个规则传过来的仓信息中是否有店仓，无则进入下个规则
        if(CollectionUtils.isEmpty(shopWarehouseWithPriorities)){
            log.warn("not shopWarehouseWithPriorities so skip");
            return Boolean.TRUE;
        }

        List<Shop> shops = Lists.newArrayListWithCapacity(shopWarehouseWithPriorities.size());
        List<Long> shopWarehouseIds = Lists.newArrayListWithCapacity(shopWarehouseWithPriorities.size());
        //拒绝过的门店
        List<Long> rejectShopIds = dispatchComponent.findRejectedShop(shopOrder.getId());
        context.put(DispatchContants.REJECT_SHOP_IDS, (Serializable) rejectShopIds);

        //封装店铺id
        shopWarehouseWithPriorities.forEach(shopWarehouseWithPrioritie ->{
            WarehouseDTO warehouse = warehouseCacher.findById(shopWarehouseWithPrioritie.getWarehouseId());
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(warehouse.getOutCode(),Long.valueOf(warehouse.getCompanyId()));
            //判断店铺状态及过滤是否拒过单
            if (com.google.common.base.Objects.equal(shop.getStatus(),1) &&! rejectShopIds.contains(shop.getId())){
                shops.add(shop);
                shopWarehouseWithPrioritie.setShopId(shop.getId());
                shopWarehouseIds.add(warehouse.getId());
            }
        });

        if (CollectionUtils.isEmpty(shops)){
            log.warn("not validShops so skip");
            return Boolean.TRUE;
        }

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(shopWarehouseIds,skuCodes, dispatchOrderItemInfo.getOpenShopId());
        if(CollectionUtils.isEmpty(skuStockInfos)){
            log.warn("not skuStockInfos so skip");
            return Boolean.TRUE;
        }
        Table<Long, String, Integer> shopSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(shopSkuCodeQuantityTable)){
            shopSkuCodeQuantityTable = HashBasedTable.create();
        }
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

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
        String addressRegion = (String) context.get(DispatchContants.BUYER_ADDRESS_REGION);

        ShopShipment shopShipment = shopAddressComponent.nearestShop(warehouses4Address.getPriorityShopIds(),shopShipments,address,addressRegion);

        dispatchOrderItemInfo.setShopShipments(Lists.newArrayList(shopShipment));

        return Boolean.FALSE;
    }

}
