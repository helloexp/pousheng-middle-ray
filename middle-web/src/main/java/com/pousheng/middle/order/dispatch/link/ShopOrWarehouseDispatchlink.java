package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.DispatchWithPriority;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
public class ShopOrWarehouseDispatchlink implements DispatchOrderLink{


    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private ShopCacher shopCacher;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-ShopOrWarehouseDispatchlink-7  order(id:{}) start...",shopOrder.getId());

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
        String addressRegion = (String) context.get(DispatchContants.BUYER_ADDRESS_REGION);

        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }

        //全部的仓和门店的距离信息
        List<DispatchWithPriority> allDispatchWithPriorities = Lists.newArrayList();

        //全部的仓和门店
        Table<String, String, Integer> allSkuCodeQuantityTable = HashBasedTable.create();


        //全部仓及商品信息
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(warehouseSkuCodeQuantityTable)){
            warehouseSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE, (Serializable) warehouseSkuCodeQuantityTable);
        }
        //调用高德地图查询地址坐标
        Location location = dispatchComponent.getLocation(address,addressRegion);

        ListMultimap<Long, String> byWarehouseId = ArrayListMultimap.create();
        //最少拆单中发货件数最多的仓
        for (Long warehouseId : warehouseSkuCodeQuantityTable.rowKeySet()) {
            //本仓库当前可以发货的数量
            /*for (String skuCode : current.elementSet()) {
                int required = current.count(skuCode);
                int stock = warehouseSkuCodeQuantityTable.get(warehouseId, skuCode);
                if(stock>=required){
                    byWarehouseId.put(warehouseId,skuCode);
                }
            }*/

            DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
            String warehouseOrShopId = "warehouse:"+warehouseId;
            dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
            dispatchWithPriority.setPriority(getDistance(warehouseId,AddressBusinessType.WAREHOUSE,location));
            allDispatchWithPriorities.add(dispatchWithPriority);
            //添加到 allSkuCodeQuantityTable
            for (String skuCode : current.elementSet()) {
                Object stockObject = warehouseSkuCodeQuantityTable.get(warehouseId,skuCode);
                Integer stock = 0;
                if(!Arguments.isNull(stockObject)){
                    stock = (Integer) stockObject;
                }
                allSkuCodeQuantityTable.put(warehouseOrShopId,skuCode,stock);
            }

        }

        List<DispatchWithPriority> warehouseDispatchWithPriority = dispatchComponent.sortDispatchWithPriority(allDispatchWithPriorities);

        //最远仓的距离
        Double farthestWarehouseDistance = 0.0;
        if(!CollectionUtils.isEmpty(warehouseDispatchWithPriority)){
            DispatchWithPriority farthestWarehouse = warehouseDispatchWithPriority.get(warehouseDispatchWithPriority.size()-1);
            farthestWarehouseDistance = farthestWarehouse.getPriority();
        }
        //全部门店及商品信息
        Table<Long, String, Integer> shopSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(shopSkuCodeQuantityTable)){
            shopSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE, (Serializable) shopSkuCodeQuantityTable);
        }
        ListMultimap<Long, String> byShopId = ArrayListMultimap.create();
        //最少拆单中发货件数最多的仓
        for (Long shopId : shopSkuCodeQuantityTable.rowKeySet()) {
            //本仓库当前可以发货的数量
            /*for (String skuCode : current.elementSet()) {
                int required = current.count(skuCode);
                int stock = shopSkuCodeQuantityTable.get(shopId, skuCode);
                if(stock>=required){
                    byShopId.put(shopId,skuCode);
                }
            }*/

            DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
            String warehouseOrShopId = "shop:"+shopId;
            dispatchWithPriority.setWarehouseOrShopId(warehouseOrShopId);
            //店铺的距离永远按比仓的距离远，实现 先仓后店
            dispatchWithPriority.setPriority(getDistance(shopId,AddressBusinessType.SHOP,location)+farthestWarehouseDistance);
            allDispatchWithPriorities.add(dispatchWithPriority);
            //添加到 allSkuCodeQuantityTable
            for (String skuCode : current.elementSet()) {
                Object stockObject =shopSkuCodeQuantityTable.get(shopId,skuCode);
                Integer stock = 0;
                if(Arguments.notNull(stockObject)){
                    stock = (Integer) stockObject;
                }
                allSkuCodeQuantityTable.put(warehouseOrShopId,skuCode,stock);
            }
        }


        List<DispatchWithPriority> allDispatchWithPriority = dispatchComponent.sortDispatchWithPriority(allDispatchWithPriorities);

        packageShipmentInfo(dispatchOrderItemInfo,allSkuCodeQuantityTable,skuCodeAndQuantities,allDispatchWithPriority);

        return false;
    }


    private void packageShipmentInfo(DispatchOrderItemInfo dispatchOrderItemInfo,Table<String, String, Integer> allSkuCodeQuantityTable,List<SkuCodeAndQuantity> skuCodeAndQuantities,List<DispatchWithPriority> dispatchWithPriorities){

        //skuCode及数量
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }
        //仓库发货单
        List<WarehouseShipment> warehouseShipmentResult = Lists.newArrayList();
        //店铺发货单
        List<ShopShipment> shopShipmentResult = Lists.newArrayList();

        //把商品派到对应的发货单上
        while (current.size() > 0) {
            //发货仓sku最大数量
            int affordCount = 0;
            String candidateWarehouseOrShopId = null;
            for (DispatchWithPriority dispatchWithPriority : dispatchWithPriorities) {
                String warehouseOrShopId = dispatchWithPriority.getWarehouseOrShopId();
                //发货单下sku数量
                int count = 0;
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = allSkuCodeQuantityTable.get(warehouseOrShopId, skuCode);
                    int actual = stock >= required ? 1 : 0;
                    count += actual;
                }
                //sku数量多说明拆单最小
                if (count > affordCount) {
                    affordCount = count; //更新当前仓库的可发货sku数量
                    //距离最近拆单最少的
                    candidateWarehouseOrShopId = warehouseOrShopId;
                }
            }
            if (Strings.isNullOrEmpty(candidateWarehouseOrShopId)) {
                List<SkuCodeAndQuantity> skuAndQuantity = Lists.newArrayList();
                //下边这些商品是没有库存
                for (String skuCode : current.elementSet()) {
                    log.warn("insufficient sku(skuCode={}) stock: ", skuCode);
                    SkuCodeAndQuantity adq = new SkuCodeAndQuantity();
                    adq.setSkuCode(skuCode);
                    adq.setQuantity(current.count(skuCode));
                    skuAndQuantity.add(adq);
                }
                dispatchOrderItemInfo.setSkuCodeAndQuantities(skuAndQuantity);
                break;
            } else {//分配发货仓库
                List<String> typeAndId = Splitters.COLON.splitToList(candidateWarehouseOrShopId);
                String type = typeAndId.get(0);
                Long id = Long.valueOf(typeAndId.get(1));

                List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = allSkuCodeQuantityTable.get(candidateWarehouseOrShopId, skuCode);
                    int actual = stock >= required ? required : 0;

                    SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
                    scaq.setSkuCode(skuCode);
                    scaq.setQuantity(actual);
                    if (actual!=0){
                        scaqs.add(scaq);
                    }

                    //减少库存需求
                    current.remove(skuCode, actual);
                }
                if(Objects.equal(type,"warehouse")){
                    WarehouseShipment warehouseShipment = new WarehouseShipment();
                    warehouseShipment.setWarehouseId(id);
                    warehouseShipment.setWarehouseName(warehouseCacher.findById(id).getName());
                    warehouseShipment.setSkuCodeAndQuantities(scaqs);
                    warehouseShipmentResult.add(warehouseShipment);

                }else {
                    ShopShipment shopShipment = new ShopShipment();
                    shopShipment.setShopId(id);
                    Shop shop = shopCacher.findShopById(id);
                    shopShipment.setShopName(shop.getName());
                    shopShipment.setSkuCodeAndQuantities(scaqs);
                    shopShipmentResult.add(shopShipment);

                }
            }

            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipmentResult);
            dispatchOrderItemInfo.setShopShipments(shopShipmentResult);
        }

    }


    /**
     * 获取距离用户收货地址到门店或仓的距离
     * @param businessId 门店或仓id
     * @param addressBusinessType {@link AddressBusinessType}
     * @param location 用户收货地址坐标
     * @return 距离
     */
    public double getDistance(Long businessId,AddressBusinessType addressBusinessType, Location location){


        AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(businessId, addressBusinessType.getValue());
        return dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()).getDistance();
    }
}