package com.pousheng.middle.web.shop.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.pousheng.middle.web.shop.dto.ShopChannel;
import com.pousheng.middle.web.shop.dto.ShopChannelGroup;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

import static com.pousheng.middle.constants.Constants.ZONE_ID;
import static com.pousheng.middle.constants.Constants.ZONE_NAME;

/**
 * Created by songrenfei on 2018/3/27
 */
@Slf4j
@Component
public class OpenShopLogic {

    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private OpenShopCacher openShopCacher;


    public List<ShopChannelGroup> findShopChannelGroup(){

        Response<List<String>> channelRes = openShopReadService.groupByChannel();
        if (!channelRes.isSuccess()) {
            log.error("fail to search all open shop by cause:{}", channelRes.getError());
            throw new ServiceException(channelRes.getError());
        }

        List<String> channels = channelRes.getResult();

        List<ShopChannel> mposChannels = Lists.newArrayList();


        List<ShopChannelGroup> channelGroups = Lists.newArrayListWithCapacity(channels.size());
        for (String channel : channels){
            ShopChannelGroup group = new ShopChannelGroup();
            group.setChannel(channel);
            List<OpenClientShop> openClientShops = findOpenShopByChannel(channel);

            List<ShopChannel> shopChannels = Lists.newArrayListWithCapacity(openClientShops.size());

            for (OpenClientShop openClientShop : openClientShops){
                ShopChannel shopChannel = new ShopChannel();
                shopChannel.setOpenClientShop(openClientShop);
                if(openClientShop.getShopName().startsWith("mpos")){
                    ShopChannel mposChannel = new ShopChannel();
                    mposChannel.setOpenClientShop(openClientShop);
                    mposChannels.add(mposChannel);
                    continue;
                }
                shopChannels.add(shopChannel);
            }

            group.setShopChannels(shopChannels);
            channelGroups.add(group);
        }

        if(!CollectionUtils.isEmpty(mposChannels)){

            ListMultimap<String, OpenClientShop> byZoneName = ArrayListMultimap.create();
            ShopChannelGroup mposGroup = new ShopChannelGroup();
            mposGroup.setChannel("mpos");

            for (ShopChannel shopChannel : mposChannels){
                OpenClientShop openClientShop = shopChannel.getOpenClientShop();
                OpenShop openShop = openShopCacher.findById(openClientShop.getOpenShopId());
                Map<String, String> extra = openShop.getExtra();
                if(Arguments.isNull(extra)||!extra.containsKey(ZONE_ID)){
                    continue;
                }
                String zoneName = extra.get(ZONE_NAME);
                byZoneName.put(zoneName,openClientShop);
            }

            List<ShopChannel> zoneChannels = Lists.newArrayListWithCapacity(byZoneName.keySet().size());

            for (String zoneName : byZoneName.keySet()){
                ShopChannel zoneChannel = new ShopChannel();
                zoneChannel.setZoneName(zoneName);
                zoneChannel.setZoneOpenClientShops(byZoneName.get(zoneName));
                zoneChannels.add(zoneChannel);
            }
            mposGroup.setShopChannels(zoneChannels);
            channelGroups.add(mposGroup);
        }

        return channelGroups;

    }


    private List<OpenClientShop> findOpenShopByChannel(String channel){
        Response<List<OpenShop>> findR = openShopReadService.findByChannel(channel);
        if (!findR.isSuccess()) {
            log.error("fail to search all open shop by cause:{}", findR.getError());
            throw new ServiceException(findR.getError());
        }
        List<OpenShop> openShops = findR.getResult();
        List<OpenClientShop> openClientShops = Lists.newArrayListWithCapacity(openShops.size());
        for (OpenShop openShop : openShops) {
            OpenClientShop openClientShop = new OpenClientShop();
            openClientShop.setShopName(openShop.getShopName());
            openClientShop.setChannel(openShop.getChannel());
            openClientShop.setOpenShopId(openShop.getId());
            openClientShops.add(openClientShop);
        }
        return openClientShops;
    }
}
