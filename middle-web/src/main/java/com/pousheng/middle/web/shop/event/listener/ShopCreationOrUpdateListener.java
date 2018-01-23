package com.pousheng.middle.web.shop.event.listener;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.order.service.AddressGpsWriteService;
import com.pousheng.middle.shop.enums.MemberFromType;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.dto.MemberCenterAddressDto;
import com.pousheng.middle.web.shop.event.CreateShopEvent;
import com.pousheng.middle.web.shop.event.UpdateShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.parana.item.SyncParanaShopService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author songrenfei
 */
@Slf4j
@Component
public class ShopCreationOrUpdateListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;
    @Autowired
    private GDMapSearchService gdMapSearchService;
    @RpcConsumer
    private AddressGpsWriteService addressGpsWriteService;
    @RpcConsumer
    private AddressGpsReadService addressGpsReadService;
    @RpcConsumer
    private ShopWriteService shopWriteService;
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private MposWarehousePusher mposWarehousePusher;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private SyncParanaShopService syncParanaShopService;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onCreated(CreateShopEvent event) {
       

        AddressGps addressGps = getAddressGps(event.getShopId(),event.getCompanyId().toString(),event.getStoreCode());
        if(Arguments.isNull(addressGps)){
            return;
        }
        Response<Long> createRes = addressGpsWriteService.create(addressGps);
        if(!createRes.isSuccess()){
            log.error("create address gps:{} fail,error:{}",addressGps,createRes.getError());
            return;
        }

        //更新门店地址信息
        updateShopAddress(event.getShopId(),addressGps.getDetail());

        try {
            //同步恒康mpos门店范围
            mposWarehousePusher.addWarehouses(event.getCompanyId().toString(),event.getOuterId());
        }catch (Exception e){
            log.error("sync hk shop(id:{}) range fail,cause:{}",event.getShopId(),Throwables.getStackTraceAsString(e));
        }

        //同步电商最新的门店地址
        Shop shop = shopCacher.findShopById(event.getShopId());
        Response<Boolean> syncParanaAddressRes = syncParanaShopService.syncShopAddress(shop.getOuterId(),
                addressGps.getProvince(),addressGps.getCity(),addressGps.getRegion(),addressGps.getDetail());
        if(!syncParanaAddressRes.isSuccess()){
            log.error("sync shop(id:{}) address to parana fail,error:{}",shop.getId(),syncParanaAddressRes.getError());
        }


    }


    @Subscribe
    public void onUpdate(UpdateShopEvent event) {

        AddressGps addressGps = getAddressGps(event.getShopId(),event.getCompanyId().toString(),event.getStoreCode());
        if(Arguments.isNull(addressGps)){
            return;
        }
        Response<AddressGps> existRes = addressGpsReadService.findByBusinessIdAndType(event.getShopId(),AddressBusinessType.SHOP);
        if(!existRes.isSuccess()){
            log.error("find address gps by businessId:{} and business type:{} fail,error:{}", event.getShopId(),AddressBusinessType.SHOP,existRes.getError());
            return;
        }
        addressGps.setId(existRes.getResult().getId());
        Response<Boolean> updateRes = addressGpsWriteService.update(addressGps);
        if(!updateRes.isSuccess()){
            log.error("updateRes address gps:{} fail,error:{}",addressGps,updateRes.getError());
            return;
        }

        //4、更新门店地址信息
        updateShopAddress(event.getShopId(),addressGps.getDetail());

        //同步电商最新的门店地址
        Shop shop = shopCacher.findShopById(event.getShopId());
        Response<Boolean> syncParanaAddressRes = syncParanaShopService.syncShopAddress(shop.getOuterId(),
                addressGps.getProvince(),addressGps.getCity(),addressGps.getRegion(),addressGps.getDetail());
        if(!syncParanaAddressRes.isSuccess()){
            log.error("sync shop(id:{}) address to parana fail,error:{}",shop.getId(),syncParanaAddressRes.getError());
        }


    }

    private void updateShopAddress(Long shopId,String address){
        Shop updateShop = new Shop();
        updateShop.setId(shopId);
        updateShop.setAddress(address);
        Response<Boolean> updateShopRes = shopWriteService.update(updateShop);
        if(!updateShopRes.isSuccess()){
            log.error("update shop:{} fail,error:{}",updateShop,updateShopRes.getError());
        }
    }
    
    private AddressGps getAddressGps(Long shopId,String companyId,String storeCode){
        //1、调用会员中心查询门店地址
        Response<MemberCenterAddressDto> addressDtoRes =  memberShopOperationLogic.findShopAddress(companyId,storeCode, MemberFromType.SHOP.value());
        if(!addressDtoRes.isSuccess()){
            log.error("find shop address by company id:{} code:{} type:{} fail,error:{}",companyId,storeCode,MemberFromType.SHOP.value(),addressDtoRes.getError());
            return null;
        }

        MemberCenterAddressDto addressDto = addressDtoRes.getResult();
        if(Arguments.isNull(addressDto)){
            log.error("not find shop address by company id:{} code:{} type:{}",companyId,storeCode,MemberFromType.SHOP.value());
            return null;
        }
        if(Strings.isNullOrEmpty(addressDto.getAddress())){
            log.error("shop address is null for company id:{} code:{} type:{}",companyId,storeCode,MemberFromType.SHOP.value());
            return null;
        }


        //2、调用高德地图查询地址坐标
        Optional<Location> locationOp = dispatchComponent.getLocation(addressDto.getAddress());
        if(!locationOp.isPresent()){
            log.error("[ADDRESS-LOCATION]:not find shop(id:{}) location by address:{}",shopId,addressDto.getAddress());
           return null;
        }
        Location location = locationOp.get();

        //3、创建门店地址定位信息
        AddressGps addressGps = new AddressGps();
        addressGps.setLatitude(location.getLat());
        addressGps.setLongitude(location.getLon());
        addressGps.setBusinessId(shopId);
        addressGps.setBusinessType(AddressBusinessType.SHOP.getValue());
        addressGps.setDetail(addressDto.getAddress());
        //省

        log.info("[START-TRANS-ADDRESS] addressDto:{}",addressDto);
        WarehouseAddress province = transToWarehouseAddress(addressDto.getProvinceCode(),addressDto.getProvinceName());
        if(Arguments.isNull(province)){
            return null;
        }
        addressGps.setProvinceId(province.getId());
        addressGps.setProvince(province.getName());

        //市
        WarehouseAddress city = transToWarehouseAddress(addressDto.getCityCode(),addressDto.getCityName());
        if(Arguments.isNull(city)){
            return null;
        }
        addressGps.setCityId(city.getId());
        addressGps.setCity(city.getName());

        //区
        WarehouseAddress region = transToWarehouseAddress(addressDto.getAreaCode(),addressDto.getAreaName());
        if(Arguments.isNull(region)){
            return null;
        }
        addressGps.setRegionId(region.getId());
        addressGps.setRegion(region.getName());

        return addressGps;
    }


    //将会员中心的地址与中台地址做比较，转换为中台的地址
    private WarehouseAddress transToWarehouseAddress(String code,String name){
        try {
            if(Strings.isNullOrEmpty(code)){
                log.error("[QUERY-MIDDLE-ADDRESS] code is null",code,name);
                return null;
            }
            Long addressId = Long.valueOf(code);
            WarehouseAddress address = warehouseAddressCacher.findById(addressId);
            if(Arguments.isNull(address)){
                log.error("[QUERY-MIDDLE-ADDRESS] by code:{} name:{} not find",code,name);
                return null;
            }
            return address;
        }catch (ServiceException e){
            log.error("check is matching warehouse address by code:{} name:{} fail,error:{}",code,name, e.getMessage());
            return null;
        }catch (Exception e){
            log.error("check is matching warehouse address by code:{} name:{} fail,cause:{}",code,name, Throwables.getStackTraceAsString(e));
            return null;
        }

    }
}
