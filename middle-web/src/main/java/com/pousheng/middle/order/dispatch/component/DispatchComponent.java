package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.dto.DispatchWithPriority;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.utils.DistanceUtil;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/12/25
 */
@Component
@Slf4j
public class DispatchComponent {

    @Autowired
    private GDMapSearchService gdMapSearchService;
    @RpcConsumer
    private MposSkuStockReadService mposSkuStockReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private ShopCacher shopCacher;


    private static final Ordering<DistanceDto> bydiscount = Ordering.natural().onResultOf(new Function<DistanceDto, Double>() {
        @Override
        public Double apply(DistanceDto input) {
            return input.getDistance();
        }
    });


    private static final Ordering<DispatchWithPriority> byPriority = Ordering.natural().onResultOf(new Function<DispatchWithPriority, Double>() {
        @Override
        public Double apply(DispatchWithPriority input) {
            return input.getPriority();
        }
    });


    public Optional<Location> getLocation(String address){
        //1、调用高德地图查询地址坐标
        Response<Optional<Location>> locationRes = gdMapSearchService.searchByAddress(address);
        if(!locationRes.isSuccess()){
            log.error("find location by address:{} fail,error:{}",address,locationRes.getError());
            throw new ServiceException(locationRes.getError());
        }

        return locationRes.getResult();
    }

    public Long getMposSkuWarehouseLockStock(Long warehouseId,String skuCode){

        Response<WarehouseSkuStock> rStock = warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
        if (!rStock.isSuccess()) {
            log.error("failed to find sku(skuCode={}) in warehouse(id={}), error code:{}",
                    skuCode, warehouseId, rStock.getError());
            throw new ServiceException(rStock.getError());
        }

        return rStock.getResult().getLockedStock();
    }



    public Long getMposSkuShopLockStock(Long warehouseId,String skuCode){

        Response<Optional<MposSkuStock>> response = mposSkuStockReadService.findByShopIdAndSkuCode(warehouseId,skuCode);
        if(!response.isSuccess()){
            log.error("find mposSkuStock by shop id :{} and sku code:{} failed,  error:{}", warehouseId,skuCode, response.getError());
            return 0L;
        }

        Optional<MposSkuStock> stockOptional = response.getResult();
        if(stockOptional.isPresent()){
            return stockOptional.get().getLockedStock();
        }

        return 0L;
    }

    /**
     * 完善 仓库商品库存信息
     * @param skuStockInfos 商品数量信息
     * @param skuCodeQuantityTable tab
     */
    public void completeWarehouseTab(List<HkSkuStockInfo> skuStockInfos, Table<Long, String, Integer> skuCodeQuantityTable){

        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos){
            Warehouse warehouse = warehouseCacher.findById(hkSkuStockInfo.getBusinessId());
            Map<String,String> extra = warehouse.getExtra();
            if(CollectionUtils.isEmpty(extra)||!extra.containsKey("safeStock")){
                log.error("not find safe stock for warehouse:(id:{})",hkSkuStockInfo.getBusinessId());
                throw new ServiceException("warehouse.safe.stock.not.find");
            }
            //安全库存
            Integer safeStock = Integer.valueOf(extra.get("safeStock"));
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                //可用库存
                Integer availStock = skuAndQuantityInfo.getQuantity();
                //锁定库存
                Integer lockStock = Integer.valueOf(this.getMposSkuWarehouseLockStock(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode()).toString());
                //这里先不考虑 availStock-lockStock - safeStock 负数情况
                skuCodeQuantityTable.put(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode(),availStock-lockStock -safeStock);
            }
        }
    }


    /**
     * 完善 门店商品库存信息
     * @param skuStockInfos 商品数量信息
     * @param skuCodeQuantityTable tab
     */
    public void completeShopTab(List<HkSkuStockInfo> skuStockInfos, Table<Long, String, Integer> skuCodeQuantityTable){

        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos){
            Shop shop = shopCacher.findShopById(hkSkuStockInfo.getBusinessId());

            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            //安全库存
            Integer safeStock = Arguments.isNull(shopExtraInfo.getSafeStock())?0:shopExtraInfo.getSafeStock();
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                //可用库存
                Integer availStock = skuAndQuantityInfo.getQuantity();
                //mpos占用库存
                Integer lockStock = Integer.valueOf(this.getMposSkuShopLockStock(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode()).toString());
                //这里先不考虑 availStock-lockStock-safeStock 负数情况
                skuCodeQuantityTable.put(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode(),availStock-lockStock-safeStock);
            }
        }
    }



    /**
     * 获取查询roger返回的仓是否有整单发货的
     * @param hkSkuStockInfos 仓及商品集合
     * @param widskucode2stock 仓、商品、数量的table
     * @param skuCodeAndQuantities 商品编码和数量
     * @return 可以整单发货的仓
     */
    public List<WarehouseShipment> chooseSingleWarehouse(List<HkSkuStockInfo> hkSkuStockInfos, Table<Long, String, Integer> widskucode2stock,
                                                         List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<WarehouseShipment> singleWarehouses = Lists.newArrayListWithCapacity(hkSkuStockInfos.size());
        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfos) {
            List<WarehouseShipment> warehouseShipments = trySingleWarehouse(skuCodeAndQuantities, widskucode2stock, skuStockInfo);
            if (!CollectionUtils.isEmpty(warehouseShipments)) {
                singleWarehouses.addAll(warehouseShipments);
            }
        }
        return singleWarehouses;
    }



    /**
     * 获取查询roger返回的门店是否有整单发货的
     * @param hkSkuStockInfos 门店及商品集合
     * @param shopskucode2stock 门店、商品、数量的table
     * @param skuCodeAndQuantities 商品编码和数量
     * @return 可以整单发货的门店
     */
    public List<ShopShipment> chooseSingleShop(List<HkSkuStockInfo> hkSkuStockInfos, Table<Long, String, Integer> shopskucode2stock,
                                                    List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<ShopShipment> singleShops = Lists.newArrayListWithCapacity(hkSkuStockInfos.size());
        for (HkSkuStockInfo skuStockInfo : hkSkuStockInfos) {
            List<ShopShipment> shopShipments = trySingleShop(skuCodeAndQuantities, shopskucode2stock, skuStockInfo);
            if (!CollectionUtils.isEmpty(shopShipments)) {
                singleShops.addAll(shopShipments);
            }
        }
        return singleShops;
    }


    public List<String> getSkuCodes(List<SkuCodeAndQuantity> skuCodeAndQuantities){
        return Lists.transform(skuCodeAndQuantities, new Function<SkuCodeAndQuantity, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuCodeAndQuantity input) {
                return input.getSkuCode();
            }
        });

    }

    public List<String> getWarehouseSkuCodes(List<WarehouseShipment> warehouseShipments){
        List<String> skuCodes = Lists.newArrayList();
        for (WarehouseShipment warehouseShipment : warehouseShipments){
            for (SkuCodeAndQuantity skuCodeAndQuantity : warehouseShipment.getSkuCodeAndQuantities()){
                skuCodes.add(skuCodeAndQuantity.getSkuCode());
            }
        }

        return skuCodes;
    }

    public List<DistanceDto> sortDistanceDto(List<DistanceDto> distanceDtos){
        return bydiscount.sortedCopy(distanceDtos);
    }


    public List<DispatchWithPriority> sortDispatchWithPriority(List<DispatchWithPriority> dispatchWithPriorities){
        return byPriority.sortedCopy(dispatchWithPriorities);
    }

    public DistanceDto getDistance(AddressGps addressGps, String longitude, String latitude){

        DistanceDto distanceDto = new DistanceDto();
        distanceDto.setDistance(DistanceUtil.getDistance(Double.valueOf(addressGps.getLatitude()),Double.valueOf(addressGps.getLongitude()),Double.valueOf(latitude),Double.valueOf(longitude)));
        distanceDto.setId(addressGps.getBusinessId());
        return distanceDto;
    }


    private List<WarehouseShipment> trySingleWarehouse(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Table<Long, String, Integer> widskucode2stock,
                                                       HkSkuStockInfo skuStockInfo) {

        Long warehouseId = skuStockInfo.getBusinessId();
        if (isEnough(skuCodeAndQuantities,widskucode2stock,skuStockInfo)) {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouseId);
            warehouseShipment.setWarehouseName(skuStockInfo.getBusinessName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(warehouseShipment);
        }
        return Collections.emptyList();
    }


    private List<ShopShipment> trySingleShop(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                       Table<Long, String, Integer> widskucode2stock,
                                                       HkSkuStockInfo skuStockInfo) {

        Long warehouseId = skuStockInfo.getBusinessId();
        if (isEnough(skuCodeAndQuantities,widskucode2stock,skuStockInfo)) {
            ShopShipment shopShipment = new ShopShipment();
            shopShipment.setShopId(warehouseId);
            shopShipment.setShopName(skuStockInfo.getBusinessName());
            shopShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(shopShipment);
        }
        return Collections.emptyList();
    }

    //是否满足整单发货
    private Boolean isEnough(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                             Table<Long, String, Integer> widskucode2stock,
                             HkSkuStockInfo skuStockInfo){

        List<HkSkuStockInfo.SkuAndQuantityInfo> materialList = skuStockInfo.getMaterial_list();
        Map<String, Integer> hkSkuQuantityMap = materialList.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(HkSkuStockInfo.SkuAndQuantityInfo::getBarcode, it -> it.getQuantity()));
        boolean enough = true;
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();

            if(!hkSkuQuantityMap.containsKey(skuCode)){
                enough = false;
                continue;
            }

            int stock = hkSkuQuantityMap.get(skuCode);
            widskucode2stock.put(skuStockInfo.getBusinessId(), skuCode, stock);
            if (stock < skuCodeAndQuantity.getQuantity()) {
                enough = false;
            }
        }

        return enough;
    }

    /**
     * 获取拒绝过该订单的店铺id集合
     * @param shopOrderId 订单id
     * @return 店铺id集合
     */
    public List<Long> findRejectedShop(Long shopOrderId){
        List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrderId);
        if(CollectionUtils.isEmpty(shipments))
            return null;
        List<Shipment> rejectedShipment = shipments.stream().filter(shipment -> Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.REJECTED.getValue())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(rejectedShipment)){
            List<Long> rejectedList = Lists.newArrayList();
            rejectedShipment.forEach(shipment -> {
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                rejectedList.add(shipmentExtra.getWarehouseId());
            });
            return rejectedList;
        }
        return null;
    }
}
