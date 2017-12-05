package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.pousheng.middle.order.dto.ActivityItem;
import com.pousheng.middle.order.dto.ActivityShop;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.enums.PoushengGiftOrderRule;
import com.pousheng.middle.order.enums.PoushengGiftQuantityRule;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.warehouse.dto.WarehouseWithPriority;
import com.pousheng.middle.warehouse.model.MiddleOpenShop;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.Lists;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 * 宝胜赠品活动列表,只有进行中的活动可以
 */
@Component
@Slf4j
public class PsGiftActivityStrategy {
    @Autowired
    private PoushengGiftActivityReadLogic poushengGiftActivityReadLogic;
    @Autowired
    private PoushengGiftActivityWriteLogic poushengGiftActivityWriteLogic;

    private static final Ordering<PoushengGiftActivity> byPrice = Ordering.natural().onResultOf(new Function<PoushengGiftActivity, Integer>() {
        @Override
        public Integer apply(PoushengGiftActivity input) {
            return input.getTotalPrice();
        }
    });
    /**
     * 获取合适的赠品
     * @param richSkusByShop
     * @param poushengGiftActivities 进行中的活动
     * @return
     */
    public List<GiftItem> getAvailGiftItems(RichSkusByShop richSkusByShop, List<PoushengGiftActivity> poushengGiftActivities){
        //获取满足金额的活动列表
        List<PoushengGiftActivity> activities = byPrice.sortedCopy(this.getAvailActivities(richSkusByShop, poushengGiftActivities));
        //如果没有合适的活动，直接返回
        if (activities.size()==0){
            return Lists.newArrayList();
        }
        //获取最优的活动
        PoushengGiftActivity activity =  activities.get(activities.size()-1);
        //限制参与人数
        if (Objects.equals(activity.getQuantityRule(),PoushengGiftQuantityRule.LIMIT_PARTICIPANTS.value())){
            try {
                //更新参与人数的数量
                poushengGiftActivityWriteLogic.updatePoushengGiftActivityParticipants(activity.getId());
            }catch (Exception e){
             log.error("update poushengGiftActivity participants failed,activity id is {},caused by {}",activity.getId(),e.getMessage());
                return Lists.newArrayList();
            }
        }
        return poushengGiftActivityReadLogic.getGiftItem(activity);
    }

    private List<PoushengGiftActivity> getAvailActivities(RichSkusByShop richSkusByShop, List<PoushengGiftActivity> poushengGiftActivities) {
        //获取金额
        Long fee = richSkusByShop.getFee();
        //获取数量
        Integer quantity = 0;
        List<RichSku> richSkus =  richSkusByShop.getRichSkus();
        //获取店铺
        Shop shop = richSkusByShop.getShop();
        //获取商品中
        List<String> skuCodes = Lists.newArrayList();

        Map<String,Integer> skuCodeAndQuantityMap = Maps.newHashMap();

        for (RichSku richSku : richSkus) {
            quantity = quantity+richSku.getQuantity();
            Sku sku = richSku.getSku();
            skuCodes.add(sku.getSkuCode());
            skuCodeAndQuantityMap.put(richSku.getSku().getSkuCode(),richSku.getQuantity());
        }

        List<PoushengGiftActivity> activities = Lists.newArrayList();

        for (PoushengGiftActivity poushengGiftActivity:poushengGiftActivities){

            //判断活动是否已经过期
            Long now = System.currentTimeMillis();//当前时间
            Long activityEndTime = poushengGiftActivity.getActivityEndAt().getTime();//活动截止时间
            if (now>=activityEndTime){
                continue;
            }
            List<ActivityShop> activityShops = poushengGiftActivityReadLogic.getActivityShopList(poushengGiftActivity);
            List<Long> shopIds = activityShops.stream().filter(Objects::nonNull).map(ActivityShop::getShopId).collect(Collectors.toList());
            //判断店铺是否满足条件
            if (!shopIds.contains(shop.getId())){
                continue;
            }
            //如果类型是需要满足金额且没有限定活动商品类型，则判断金额是否满足活动
            if (Objects.equals(poushengGiftActivity.getOrderRule(), PoushengGiftOrderRule.SATIFIED_FEE_IGINORE_ACTIVITY_ITEM.value())){
                if (fee<poushengGiftActivity.getOrderFee()){
                    continue;
                }
            }
            //如果类型是需要满足订单数量且不限定活动商品，则判断数量是否满足活动
            if (Objects.equals(poushengGiftActivity.getOrderRule(), PoushengGiftOrderRule.SATIFIED_QUANTITY_IGINORE_ACTIVITY_ITEM.value())){
                if (quantity<poushengGiftActivity.getOrderQuantity()){
                    continue;
                }
            }

            //如果类型是需要满足金额且有限定活动商品类型，则判断商品及商品数量是否满足 且判断金额是否满足活动
            if (Objects.equals(poushengGiftActivity.getOrderRule(), PoushengGiftOrderRule.SATIFIED_FEE_NOT_IGINORE_ACTIVITY_ITEM.value())){
                //判断该订单是否有商品在活动商品中
                if (fee<poushengGiftActivity.getOrderFee()){
                    continue;
                }
                if (!getActivitySkuCodes(skuCodeAndQuantityMap,poushengGiftActivity)){
                    continue;
                }

            }
            //如果类型是需要满足数量且有限定活动商品类型，则判断商品是否满足及商品数量是否满足,且判断数量是否满足活动
            if (Objects.equals(poushengGiftActivity.getOrderRule(), PoushengGiftOrderRule.SATIFIED_QUANTITY_NOT_IGINORE_ACTIVITY_ITEM.value())){
                //判断该订单是否有商品在活动商品中
                if (quantity<poushengGiftActivity.getOrderQuantity()){
                    continue;
                }
                if (!getActivitySkuCodes(skuCodeAndQuantityMap,poushengGiftActivity)){
                    continue;
                }

            }
            //判断是否满足活动人数的限制
            if (Objects.equals(poushengGiftActivity.getQuantityRule(), PoushengGiftQuantityRule.LIMIT_PARTICIPANTS)) {
                if (poushengGiftActivity.getAlreadyActivityQuantity()>=poushengGiftActivity.getActivityQuantity()){
                    continue;
                }
            }
            //判断赠品中是否含有订单中的sku,如果有则跳过该活动
            if(isGiftSkuEqualsOrderSku(skuCodeAndQuantityMap,poushengGiftActivity)){
                continue;
            }
            activities.add(poushengGiftActivity);
        }
        return activities;
    }

    /**
     * 判断订单中商品是否满足活动商品的件数
     * @param skuCodeAndQuantityMap 订单中的skuCode以及数量
     * @param poushengGiftActivity 赠品活动规则
     * @return
     */
    private boolean getActivitySkuCodes(Map<String,Integer> skuCodeAndQuantityMap, PoushengGiftActivity poushengGiftActivity){
        //获取活动商品信息
        List<ActivityItem> activityItems = poushengGiftActivityReadLogic.getActivityItem(poushengGiftActivity);
        //获取活动商品skuCode以及数量
        Map<String,Integer> activitySkuCodeAndQuantityMap = Maps.newHashMap();
        activityItems.forEach(activityItem -> {
            activitySkuCodeAndQuantityMap.put(activityItem.getSkuCode(),activityItem.getQuantity());
        });
        boolean isMatchActivityItem = false;
        //判断订单中商品是否在活动商品中,以及订单中商品数量是否满足活动商品数量,只要有一个满足即可
        for (String skuCode:skuCodeAndQuantityMap.keySet()){
            if ((activitySkuCodeAndQuantityMap.containsKey(skuCode))&&
                    (activitySkuCodeAndQuantityMap.get(skuCode)<=skuCodeAndQuantityMap.get(skuCode)) ){
                isMatchActivityItem = true;
            }
        }
        return isMatchActivityItem;
    }

    /**
     * 判断赠品中是否含有购买的商品，如果含有返回true，如果不含有返回false，返回true代表这个活动不满足该订单
     * @param skuCodeAndQuantityMap 订单中的skuCode以及数量
     * @param poushengGiftActivity 赠品活动规则
     * @return
     */
    private boolean isGiftSkuEqualsOrderSku(Map<String,Integer> skuCodeAndQuantityMap, PoushengGiftActivity poushengGiftActivity){
        //获取赠品信息
        List<GiftItem> giftItems = poushengGiftActivityReadLogic.getGiftItem(poushengGiftActivity);
        //获取赠品skuCode
        List<String> giftSkuCodes = Lists.newArrayList();
        giftItems.forEach(giftItem -> {
            giftSkuCodes.add(giftItem.getSkuCode());
        });
        boolean isGiftSkuEqualsOrderSku = false;
        for (String skuCode:skuCodeAndQuantityMap.keySet()){
            if (giftSkuCodes.contains(skuCode)){
                isGiftSkuEqualsOrderSku = true;
            }
        }
        return isGiftSkuEqualsOrderSku;
    }
}
