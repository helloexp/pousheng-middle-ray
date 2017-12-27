package com.pousheng.middle.order.dispatch.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
     * 获取距离用户收货地址最近的仓
     * @param warehouseShipments 发货仓集合
     * @param address 用户收货地址
     * @return 距离最近的发货仓
     */
    public WarehouseShipment nearestWarehouse(List<WarehouseShipment> warehouseShipments, String address){

        //1、调用高德地图查询地址坐标
        Location location = dispatchComponent.getLocation(address);

        List<DistanceDto> distanceDtos = Lists.newArrayListWithCapacity(warehouseShipments.size());
        for (WarehouseShipment warehouseShipment : warehouseShipments){
            AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(warehouseShipment.getWarehouseId(),AddressBusinessType.WAREHOUSE.getValue());
            distanceDtos.add(dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()));
        }

        //增序
        List<DistanceDto> sortDistance= dispatchComponent.sortDistanceDto(distanceDtos);

        Map<Long, WarehouseShipment> warehouseShipmentMap = warehouseShipments.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(WarehouseShipment::getWarehouseId, it -> it));


        return warehouseShipmentMap.get(sortDistance.get(0));

    }





}
