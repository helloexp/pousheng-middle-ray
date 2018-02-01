package com.pousheng.middle.web.shop.event.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.order.service.AddressGpsWriteService;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.event.CreateShopEvent;
import com.pousheng.middle.web.shop.event.UpdateShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.parana.item.SyncParanaShopService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.extern.slf4j.Slf4j;
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
    private GDMapSearchService gdMapSearchService;
    @RpcConsumer
    private AddressGpsWriteService addressGpsWriteService;
    @RpcConsumer
    private AddressGpsReadService addressGpsReadService;
    @RpcConsumer
    private ShopWriteService shopWriteService;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;
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
       

        AddressGps addressGps = memberShopOperationLogic.getAddressGps(event.getShopId(),event.getCompanyId().toString(),event.getStoreCode());
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

        AddressGps addressGps = memberShopOperationLogic.getAddressGps(event.getShopId(),event.getCompanyId().toString(),event.getStoreCode());
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



}
