package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
public class WarehouseAddressComponent {


    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;
    @Autowired
    private GDMapSearchService gdMapSearchService;
    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseCacher warehouseCacher;



    public List<AddressGps> findWarehouseAddressGps(Long provinceId){

        Response<List<AddressGps>> addressGpsListRes = addressGpsReadService.findByProvinceIdAndBusinessType(provinceId, AddressBusinessType.WAREHOUSE);
        if(!addressGpsListRes.isSuccess()){
            log.error("find addressGps by province id :{} for warehouse failed,  error:{}", provinceId,addressGpsListRes.getError());
            throw new ServiceException(addressGpsListRes.getError());
        }
        return addressGpsListRes.getResult();

    }

    public List<Warehouse> findWarehouseByIds(List<Long> ids){

        Response<List<Warehouse>> warehouseListRes = warehouseReadService.findByIds(ids);
        if(!warehouseListRes.isSuccess()){
            log.error("find warehouse by ids:{} failed,  error:{}", ids,warehouseListRes.getError());
            throw new ServiceException(warehouseListRes.getError());
        }
        return warehouseListRes.getResult();

    }

    /**
     * 获取优先级最高的总仓
     * @param warehouseWithPriorities 按优先级排序后的仓列表(换有多个可以整单发的仓)
     * @param warehouseShipments 可以整单发的仓
     * @return 仓发信息
     */
    public WarehouseShipment priorityWarehouse(List<WarehouseWithPriority> warehouseWithPriorities,
                                               List<WarehouseShipment> warehouseShipments){
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        //优先级最高的总仓
        Long warehouseId = warehouseWithPriorities.get(0).getWarehouseId();
        for (WarehouseShipment warehouseShipment : warehouseShipments) {
            if (Objects.equals(warehouseShipment.getWarehouseId(),warehouseId)) {
                return warehouseShipment;
            }
        }
        throw new ServiceException("calculate.warehouse.shipment.fail");

    }

    /**
     * 获取优先级最高的店仓
     * @param warehouseWithPriorities 按优先级排序后的仓列表(换有多个可以整单发的仓)
     * @param shopShipments 店仓发货商品信息
     * @return 仓发信息
     */
    public ShopShipment priorityShop(List<WarehouseWithPriority> warehouseWithPriorities,
                                     List<ShopShipment> shopShipments){
        //首先根据优先级检查仓库, 如果可以有整仓发货, 则就从那个仓发货
        //优先级最高的店ID
        for(WarehouseWithPriority w:warehouseWithPriorities){
            Long shopId = w.getShopId();
            for (ShopShipment shopShipment : shopShipments) {
                if (Objects.equals(shopShipment.getShopId(),shopId)) {
                    return shopShipment;
                }
            }
        }
        throw new ServiceException("calculate.shop.shipment.fail");

    }



    /**
     * 获取距离用户收货地址最近的仓
     * @param warehouseShipments 发货仓集合
     * @param address 用户收货地址
     * @return 距离最近的发货仓
     */
    public WarehouseShipment nearestWarehouse(List<WarehouseShipment> warehouseShipments, String address){

        //1、调用高德地图查询地址坐标
        Optional<Location> locationOp = dispatchComponent.getLocation(address);
        if(!locationOp.isPresent()){
            log.error("not find location by address:{}",address);
            throw new ServiceException("buyer.receive.info.address.invalid");
        }
        Location location = locationOp.get();

        List<DistanceDto> distanceDtos = Lists.newArrayListWithCapacity(warehouseShipments.size());
        for (WarehouseShipment warehouseShipment : warehouseShipments){
            try {
                AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(warehouseShipment.getWarehouseId(),AddressBusinessType.WAREHOUSE.getValue());
                distanceDtos.add(dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()));
            }catch (Exception e){
                log.error("find address gps by business id:{} and type:{} fail,cause:{}",warehouseShipment.getWarehouseId(),AddressBusinessType.WAREHOUSE.getValue(), Throwables.getStackTraceAsString(e));
                throw new ServiceException("address.gps.not.found");
            }
        }

        //增序
        List<DistanceDto> sortDistance= dispatchComponent.sortDistanceDto(distanceDtos);

        Map<Long, WarehouseShipment> warehouseShipmentMap = warehouseShipments.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(WarehouseShipment::getWarehouseId, it -> it));


        return warehouseShipmentMap.get(sortDistance.get(0).getId());

    }





}
