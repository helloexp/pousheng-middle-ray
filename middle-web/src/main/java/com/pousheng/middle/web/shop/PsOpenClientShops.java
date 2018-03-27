package com.pousheng.middle.web.shop;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.dto.ShopChannel;
import com.pousheng.middle.web.shop.dto.ShopChannelGroup;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.pousheng.middle.constants.Constants.ZONE_ID;
import static com.pousheng.middle.constants.Constants.ZONE_NAME;

/**
 * Created by cp on 6/16/17.
 */
@RestController
@RequestMapping("/api/open-client")
@Slf4j
public class PsOpenClientShops {

    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;

    /**
     * 查询所有店铺
     * @return
     */
    @RequestMapping(value = "/shop/all/group", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopChannelGroup> searchAllShopsGroup() {

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
                Map<String, String>  extra = openShop.getExtra();
                if(!extra.containsKey(ZONE_ID)){
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



    @RequestMapping(value = "/fix/zone", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String fixZone() {
        Response<List<OpenShop>> findR = openShopReadService.findByChannel("official");
        if (!findR.isSuccess()) {
            log.error("fail to search all open shop by cause:{}", findR.getError());
            throw new ServiceException(findR.getError());
        }
        List<OpenShop> openShops = findR.getResult();
        for (OpenShop openShop : openShops) {
            if(!openShop.getShopName().startsWith("mpos")) {
                continue;
            }

            Map<String, String>  extra = openShop.getExtra();

            List<String> keys = Splitter.on("-").splitToList(openShop.getAppKey());
            try {
                Optional<MemberShop> memberShopOptional = memberShopOperationLogic.findShopByCodeAndType(keys.get(1),1,keys.get(0));
                if(memberShopOptional.isPresent()){
                    MemberShop memberShop = memberShopOptional.get();
                    extra.put(ZONE_ID,memberShop.getZoneId());
                    extra.put(ZONE_NAME,memberShop.getZoneName());

                    OpenShop update = new OpenShop();
                    update.setId(openShop.getId());
                    update.setExtra(extra);

                    openShopWriteService.update(update);
                }
            }catch (JsonResponseException e){
                log.error("find shop by code:{}, type:{},companyId:{} fail,error:{}",keys.get(1),1,keys.get(0),e.getMessage());
            }
        }
        return "success";
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
