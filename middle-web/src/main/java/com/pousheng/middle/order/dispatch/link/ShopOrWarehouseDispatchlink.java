package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.DispatchWithPriority;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.utils.WarehouseChooseUtil;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.enums.WarehouseRuleItemPriorityType;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 门店或仓 发货规则（最后一条规则，最复杂场景）
 * 优先级 7
 * 1、最少拆单
 * 2、先仓后端
 * 3、相同拆单情况下距离最短优先
 * 4、组合拆单情况下距离和最短优先
 * Created by songrenfei on 2017/12/23
 */
@Component
@Slf4j
public class ShopOrWarehouseDispatchlink implements DispatchOrderLink {


    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private MiddleShopCacher middleShopCacher;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-ShopOrWarehouseDispatchlink-7  order(id:{}) start...",shopOrder.getId());
        Boolean oneCompany = (Boolean) context.get(DispatchContants.ONE_COMPANY);
        //如果是vip的单子 则不走拆单 如果还是同公司环节则返回true 如果是非同公司 则返回false
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.VIPOXO.getValue())) {
            if (oneCompany) {
                return Boolean.TRUE;
            } else {
                dispatchOrderItemInfo.setSkuCodeAndQuantities(skuCodeAndQuantities);
                return Boolean.FALSE;
            }
        }
        //如果是京东货到付款订单，则不走拆单逻辑 直接返回
        if (Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())) {
            dispatchOrderItemInfo.setSkuCodeAndQuantities(skuCodeAndQuantities);
            if (oneCompany) {
                return Boolean.TRUE;
            } else {
                dispatchOrderItemInfo.setSkuCodeAndQuantities(skuCodeAndQuantities);
                return Boolean.FALSE;
            }
        }
        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }
        //全部仓及商品信息
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);
        if (Arguments.isNull(warehouseSkuCodeQuantityTable)) {
            warehouseSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);
        }
        //根据距离优先级类型走不同的处理逻辑
        handleDispatchWithPriority(dispatchOrderItemInfo, skuCodeAndQuantities, warehouseSkuCodeQuantityTable, current, context);

        if (oneCompany && dispatchOrderItemInfo.getSkuCodeAndQuantities().size() > 0) {
            dispatchOrderItemInfo.setShopShipments(Lists.newArrayList());
            dispatchOrderItemInfo.setWarehouseShipments(Lists.newArrayList());
            dispatchOrderItemInfo.setSkuCodeAndQuantities(Lists.newArrayList());
            return Boolean.TRUE;
        }
        return false;
    }


    private void handleDispatchWithPriority(DispatchOrderItemInfo dispatchOrderItemInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Table<Long, String, Integer> warehouseSkuCodeQuantityTable,
                                            Multiset<String> current, Map<String, Serializable> context) {

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
        String addressRegion = (String) context.get(DispatchContants.BUYER_ADDRESS_REGION);
        Warehouses4Address warehouses4Address = (Warehouses4Address) context.get(DispatchContants.WAREHOUSE_FOR_ADDRESS);

        //全部的仓和门店的距离或优先级信息
        List<DispatchWithPriority> allDispatchWithPriorities = Lists.newArrayList();
        //优先仓
        List<DispatchWithPriority> priorityWarehouseDispatchs = Lists.newArrayList();
        //仓的距离信息
        List<DispatchWithPriority> warehouseDispatchWithPriorities = Lists.newArrayList();
        //门店的距离信息
        List<DispatchWithPriority> shopDispatchWithPriorities = Lists.newArrayList();
        //优先店
        List<DispatchWithPriority> priorityShopDispatchs = Lists.newArrayList();

        //全部的仓和门店
        Table<String, String, Integer> allSkuCodeQuantityTable = HashBasedTable.create();

        //调用高德地图查询地址坐标
        Location location = dispatchComponent.getLocation(address, addressRegion);

        if (!CollectionUtils.isEmpty(warehouses4Address.getPriorityWarehouseIds())) {
            Map<Integer, List<Long>> priorityWarehouseMap = warehouses4Address.getPriorityWarehouseIds();
            for (Map.Entry<Integer, List<Long>> entry : priorityWarehouseMap.entrySet()) {
                List<Long> priorityWarehouseIds = entry.getValue();
                //优先仓
                List<DispatchWithPriority> priorityWarehouse = Lists.newArrayList();
                for (Long warehouseId : priorityWarehouseIds) {

                    //仓的距离信息
                    if (!warehouseSkuCodeQuantityTable.containsRow(warehouseId)) {
                        continue;
                    }
                    DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
                    String warehouseOrShopId = "warehouse:" + warehouseId;
                    dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
                    dispatchWithPriority.setDistance(getDistance(warehouseId, AddressBusinessType.WAREHOUSE, location));
                    priorityWarehouse.add(dispatchWithPriority);

                    fillAllSkuCodeQuantityTable(current, warehouseSkuCodeQuantityTable, warehouseId, warehouseOrShopId, allSkuCodeQuantityTable);
                    for (String skuCode : current.elementSet()) {
                        warehouseSkuCodeQuantityTable.remove(warehouseId, skuCode);
                    }
                }
                if (priorityWarehouse.size() > 1) {
                    priorityWarehouse = dispatchComponent.sortDispatchWithDistance(priorityWarehouse);
                }
                priorityWarehouseDispatchs.addAll(priorityWarehouse);
            }

        }

        //最少拆单中发货件数最多的仓
        for (Long warehouseId : warehouseSkuCodeQuantityTable.rowKeySet()) {
            DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
            String warehouseOrShopId = "warehouse:" + warehouseId;
            dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
            //根据距离优先级派单
            dispatchWithPriority.setDistance(getDistance(warehouseId, AddressBusinessType.WAREHOUSE, location));
            warehouseDispatchWithPriorities.add(dispatchWithPriority);
            fillAllSkuCodeQuantityTable(current, warehouseSkuCodeQuantityTable, warehouseId, warehouseOrShopId, allSkuCodeQuantityTable);
        }

        List<DispatchWithPriority> warehouseDispatchWithPriority = dispatchComponent.sortDispatchWithDistance(warehouseDispatchWithPriorities);

        //最远仓的距离
        Double farthestWarehouseDistance = 0.0;
        if (!CollectionUtils.isEmpty(warehouseDispatchWithPriority)) {
            DispatchWithPriority farthestWarehouse = warehouseDispatchWithPriority.get(warehouseDispatchWithPriority.size() - 1);
            farthestWarehouseDistance = farthestWarehouse.getDistance();
        }
        //全部门店及商品信息
        Table<Long, String, Integer> shopSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE);
        if (Arguments.isNull(shopSkuCodeQuantityTable)) {
            shopSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE, (Serializable) shopSkuCodeQuantityTable);
        }

        if (!CollectionUtils.isEmpty(warehouses4Address.getPriorityShopIds())) {
            Map<Integer, List<Long>> priorityShopMap = warehouses4Address.getPriorityShopIds();
            for (Map.Entry<Integer, List<Long>> entry : priorityShopMap.entrySet()) {
                List<Long> priorityShopIds = entry.getValue();
                List<DispatchWithPriority> priorityShop = Lists.newArrayList();
                for (Long shopId : priorityShopIds) {
                    if (!shopSkuCodeQuantityTable.containsRow(shopId)) {
                        continue;
                    }
                    DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
                    String warehouseOrShopId = "shop:" + shopId;
                    dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
                    //店铺的距离永远按比仓的距离远，实现 先仓后店
                    dispatchWithPriority.setDistance(getDistance(shopId, AddressBusinessType.SHOP, location) + farthestWarehouseDistance);
                    priorityShop.add(dispatchWithPriority);
                    fillAllSkuCodeQuantityTable(current, shopSkuCodeQuantityTable, shopId, warehouseOrShopId, allSkuCodeQuantityTable);
                    for (String skuCode : current.elementSet()) {
                        shopSkuCodeQuantityTable.remove(shopId, skuCode);
                    }
                }
                if (priorityShop.size() > 1) {
                    priorityShop = dispatchComponent.sortDispatchWithDistance(priorityShop);
                }
                priorityShopDispatchs.addAll(priorityShop);
            }
        }

        //最少拆单中发货件数最多的仓
        for (Long shopId : shopSkuCodeQuantityTable.rowKeySet()) {
            DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
            String warehouseOrShopId = "shop:" + shopId;
            dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
            //店铺的距离永远按比仓的距离远，实现 先仓后店
            dispatchWithPriority.setDistance(getDistance(shopId, AddressBusinessType.SHOP, location) + farthestWarehouseDistance);
            shopDispatchWithPriorities.add(dispatchWithPriority);
            fillAllSkuCodeQuantityTable(current, shopSkuCodeQuantityTable, shopId, warehouseOrShopId, allSkuCodeQuantityTable);


        }

        List<DispatchWithPriority> shopDispatchWithPriority = dispatchComponent.sortDispatchWithDistance(shopDispatchWithPriorities);


        allDispatchWithPriorities.addAll(priorityWarehouseDispatchs);
        allDispatchWithPriorities.addAll(warehouseDispatchWithPriority);
        allDispatchWithPriorities.addAll(priorityShopDispatchs);
        allDispatchWithPriorities.addAll(shopDispatchWithPriority);
        //TODO 测试用 通过后删除
        log.info("this priorityWarehouseDispatchs  is {}", priorityWarehouseDispatchs.toString());
        log.info("this warehouseDispatchWithPriority  is {}", warehouseDispatchWithPriority.toString());
        log.info("this priorityShopDispatchs  is {}", priorityShopDispatchs.toString());
        log.info("this shopDispatchWithPriority  is {}", shopDispatchWithPriority.toString());
        log.info("this allDispatchWithPriorities  is {}", allDispatchWithPriorities.toString());
        log.info("allSkuCodeQuantityTable is {}", allSkuCodeQuantityTable.toString());
        log.info("skuCodeAndQuantities is {}", allSkuCodeQuantityTable.toString());
        packageShipmentInfo(dispatchOrderItemInfo, allSkuCodeQuantityTable, skuCodeAndQuantities, allDispatchWithPriorities);

    }

    /**
     * 添加到 allSkuCodeQuantityTable
     */
    private void fillAllSkuCodeQuantityTable(Multiset<String> current, Table<Long, String, Integer> skuCodeQuantityTable, Long id, String warehouseOrShopId, Table<String, String, Integer> allSkuCodeQuantityTable) {
        for (String skuCode : current.elementSet()) {
            Object stockObject = skuCodeQuantityTable.get(id, skuCode);
            Integer stock = 0;
            if (!Arguments.isNull(stockObject)) {
                stock = (Integer) stockObject;
            }
            allSkuCodeQuantityTable.put(warehouseOrShopId, skuCode, stock);
        }
    }

    private Boolean isDistanceType(Warehouses4Address warehouses4Address) {
        //优先级类型
        WarehouseRuleItemPriorityType priorityType = WarehouseRuleItemPriorityType.from(warehouses4Address.getWarehouseRule().getItemPriorityType());
        switch (priorityType) {
            case DISTANCE:
                return Boolean.TRUE;
            case PRIORITY:
                return Boolean.FALSE;
        }
        log.error("warehouses4Address priority type:{} invalid", warehouses4Address.getWarehouseRule().getItemPriorityType());
        throw new ServiceException("priority.type.invalid");
    }


    private void packageShipmentInfo(DispatchOrderItemInfo dispatchOrderItemInfo, Table<String, String, Integer> allSkuCodeQuantityTable, List<SkuCodeAndQuantity> skuCodeAndQuantities, List<DispatchWithPriority> dispatchWithPriorities) {


        //仓库发货单
        List<WarehouseShipment> warehouseShipmentResult = Lists.newArrayList();
        //店铺发货单
        List<ShopShipment> shopShipmentResult = Lists.newArrayList();

        ArrayListMultimap<String, SkuCodeAndQuantity> skuCodeAndQuantityMultimap
                = WarehouseChooseUtil.collectSkuCodeAndQuantityMultimap(skuCodeAndQuantities);

        //把商品派到对应的发货单上
        while (skuCodeAndQuantityMultimap.size() > 0) {
            String candidateWarehouseOrShopId = WarehouseChooseUtil.getWarehouseIdForPackage(
                    dispatchWithPriorities, allSkuCodeQuantityTable, skuCodeAndQuantityMultimap);
            if (Strings.isNullOrEmpty(candidateWarehouseOrShopId)) {
                //下边这些商品是没有库存
                log.warn("stock insufficient of skuCodeAndQuantities: {}", skuCodeAndQuantities);
                dispatchOrderItemInfo.setSkuCodeAndQuantities(skuCodeAndQuantities);
                break;
            } else {//分配发货仓库
                List<String> typeAndId = Splitters.COLON.splitToList(candidateWarehouseOrShopId);
                String type = typeAndId.get(0);
                Long id = Long.valueOf(typeAndId.get(1));
                String shopName = null;
                String warehouseName = null;
                if (Objects.equals(type, "warehouse")) {
                    warehouseName = warehouseCacher.findById(id).getWarehouseName();
                } else {
                    shopName = shopCacher.findShopById(id).getName();
                }
                WarehouseChooseUtil.assignWarehouseShipment(skuCodeAndQuantityMultimap, warehouseShipmentResult,
                        shopShipmentResult, allSkuCodeQuantityTable, candidateWarehouseOrShopId, warehouseName, shopName);
            }
            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipmentResult);
            dispatchOrderItemInfo.setShopShipments(shopShipmentResult);
        }

    }


    /**
     * 获取距离用户收货地址到门店或仓的距离
     *
     * @param businessId          门店或仓id
     * @param addressBusinessType {@link AddressBusinessType}
     * @param location            用户收货地址坐标
     * @return 距离
     */
    public double getDistance(Long businessId, AddressBusinessType addressBusinessType, Location location) {

        try {
            AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(businessId, addressBusinessType.getValue());
            return dispatchComponent.getDistance(addressGps, location.getLon(), location.getLat()).getDistance();
        } catch (Exception e) {
            log.error("find address gps by business id:{} and type:{} fail,cause:{}", businessId, addressBusinessType.getValue(), Throwables.getStackTraceAsString(e));
            throw new ServiceException("address.gps.not.found");
        }
    }
}
