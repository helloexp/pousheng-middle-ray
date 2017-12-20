package com.pousheng.middle.web.shop.event.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.web.shop.event.CreateShopEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author songrenfei
 */
@Slf4j
@Component
public class CreateOpenShopRelationListener {

    @Autowired
    private EventBus eventBus;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private ShopWriteService shopWriteService;


    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void createOpenShopRelation(CreateShopEvent event) {

        val rExist = shopReadService.findById(event.getShopId());
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",event.getShopId(),rExist.getError());
            return;
        }
        Shop exist = rExist.getResult();

        //创建open shop 关系

        OpenShop openShop = new OpenShop();
        openShop.setChannel(ShopConstants.CHANNEL);
        openShop.setShopName(exist.getName());
        openShop.setAccessToken("xxx");
        openShop.setAppKey("xxx");
        openShop.setGateway("xxx");
        openShop.setSecret("xxx");

        Response<Long> openShopRes = openShopWriteService.create(openShop);
        if(!openShopRes.isSuccess()){
            log.error("create open shop :{} fail,error:{}",openShop,openShopRes.getError());
            return;
        }

        ShopExtraInfo shopExtraInfo =  ShopExtraInfo.fromJson(exist.getExtra());
        shopExtraInfo.setOpenShopId(openShopRes.getResult());

        Shop updateShop = new Shop();
        updateShop.setId(event.getShopId());
        updateShop.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),shopExtraInfo));

        shopWriteService.update(updateShop);

    }


}
