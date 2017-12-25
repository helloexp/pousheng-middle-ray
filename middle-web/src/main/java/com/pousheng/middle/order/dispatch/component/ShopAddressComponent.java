package com.pousheng.middle.order.dispatch.component;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.dto.DistanceDto;
import com.pousheng.middle.order.dispatch.dto.ShopShipment;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
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
public class ShopAddressComponent {


    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @Autowired
    private GDMapSearchService gdMapSearchService;
    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;




    public List<AddressGps> findShopAddressGps(Long provinceId){

        Response<List<AddressGps>> addressGpsListRes = addressGpsReadService.findByProvinceIdAndBusinessType(provinceId, AddressBusinessType.SHOP);
        if(!addressGpsListRes.isSuccess()){
            log.error("find addressGps by province id :{} for shop failed,  error:{}", provinceId,addressGpsListRes.getError());
            throw new ServiceException(addressGpsListRes.getError());
        }
        return addressGpsListRes.getResult();

    }


    public List<Shop> findShopByIds(List<Long> ids){

        Response<List<Shop>> shopListRes = shopReadService.findByIds(ids);
        if(!shopListRes.isSuccess()){
            log.error("find shop by ids:{} failed,  error:{}", ids,shopListRes.getError());
            throw new ServiceException(shopListRes.getError());
        }
        return shopListRes.getResult();

    }


    /**
     * 获取距离用户收货地址最近的门店
     * @param shopShipments 发货门店集合
     * @param address 用户收货地址
     * @return 距离最近的发货门店
     */
    public ShopShipment nearestShop(List<ShopShipment> shopShipments, String address){

        //1、调用高德地图查询地址坐标
        Response<Optional<Location>>  locationRes = gdMapSearchService.searchByAddress(address);
        if(!locationRes.isSuccess()){
            log.error("find location by address:{} fail,error:{}",address,locationRes.getError());
            throw new ServiceException(locationRes.getError());
        }

        Optional<Location> locationOp = locationRes.getResult();
        if(!locationOp.isPresent()){
            log.error("not find location by address:{}",address);
            return null;
        }
        Location location = locationOp.get();

        List<DistanceDto> distanceDtos = Lists.newArrayListWithCapacity(shopShipments.size());
        for (ShopShipment shopShipment : shopShipments){
            AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(shopShipment.getShopId(),AddressBusinessType.SHOP.getValue());
            distanceDtos.add(dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()));
        }

        //增序
        List<DistanceDto> sortDistance= dispatchComponent.sortDistanceDto(distanceDtos);

        Map<Long, ShopShipment> shopShipmentMap = shopShipments.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShopShipment::getShopId, it -> it));


        return shopShipmentMap.get(sortDistance.get(0));

    }







}
