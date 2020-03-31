package com.pousheng.middle.web.shop.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.pousheng.middle.constants.Constants.ZONE_ID;
import static com.pousheng.middle.constants.Constants.ZONE_NAME;

@Slf4j
@Component
public class SyncOpenShopLogic {
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private ShopWriteService shopWriteService;
    @Value("${open.api.gateway}")
    private String gateway;
    
    public void createOpenShopRelations(Long shopId){
        log.info("SYNCcreateOpenShopRelation shopID:{}",shopId);

        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            return;
        }
        Shop exist = rExist.getResult();

        //创建open shop 关系

        OpenShop openShop = new OpenShop();
        openShop.setChannel(ShopConstants.CHANNEL);
        openShop.setShopName("mpos-"+exist.getName());
        openShop.setAccessToken("xxx");
        openShop.setAppKey(exist.getBusinessId()+"-"+exist.getOuterId());
        openShop.setStatus(1);
        openShop.setGateway(gateway);
        openShop.setSecret(exist.getOuterId()+"93204aefe45d47f6e488");
        Map<String,String> openExtra = Maps.newHashMap();
        openExtra.put("isOrderInsertMiddle","false");
        openExtra.put(ZONE_ID,exist.getZoneId());
        openExtra.put(ZONE_NAME,exist.getZoneName());
        openExtra.put(TradeConstants.ERP_SYNC_TYPE,"yyEdi");
        openShop.setExtra(openExtra);


        Response<Long> openShopRes = openShopWriteService.create(openShop);
        if(!openShopRes.isSuccess()){
            log.error("create open shop :{} fail,error:{}",openShop,openShopRes.getError());
            return;
        }

        ShopExtraInfo shopExtraInfo =  ShopExtraInfo.fromJson(exist.getExtra());
        shopExtraInfo.setOpenShopId(openShopRes.getResult());

        Shop updateShop = new Shop();
        updateShop.setId(shopId);
        updateShop.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),shopExtraInfo));

        shopWriteService.update(updateShop);
    }
    
}
