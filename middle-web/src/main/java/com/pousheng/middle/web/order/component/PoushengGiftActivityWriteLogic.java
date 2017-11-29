package com.pousheng.middle.web.order.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ActivityItem;
import com.pousheng.middle.order.dto.ActivityShop;
import com.pousheng.middle.order.dto.EditSubmitGiftActivityInfo;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.dto.fsm.PoushengGiftActivityStatus;
import com.pousheng.middle.order.enums.PoushengGiftOrderRule;
import com.pousheng.middle.order.enums.PoushengGiftQuantityRule;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 * @author tony
 */
@Component
@Slf4j
public class PoushengGiftActivityWriteLogic {
    @Autowired
    private PoushengGiftActivityWriteService poushengGiftActivityWriteService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    /**
     * 创建活动规则
     * @param editSubmitGiftActivityInfo
     * @return
     */
    public long createGiftActivity(EditSubmitGiftActivityInfo editSubmitGiftActivityInfo)
    {
        //活动店铺
        List<ActivityShop> activityShops = editSubmitGiftActivityInfo.getActivityShops();
        //赠品
        List<GiftItem> giftItems = editSubmitGiftActivityInfo.getGiftItems();
        //活动商品
        List<ActivityItem> activityItems = editSubmitGiftActivityInfo.getActivityItems();

        PoushengGiftActivity activity = new PoushengGiftActivity();
        activity.setName(editSubmitGiftActivityInfo.getName());
        //满足金额且不限活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),1)&&editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_FEE_IGINORE_ACTIVITY_ITEM.value());
        }
        //满足金额且限定活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),1)&&!editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_FEE_NOT_IGINORE_ACTIVITY_ITEM.value());
            activity.setOrderFee(editSubmitGiftActivityInfo.getFee());
        }
        //满足数量且不限活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),2)&&editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_QUANTITY_IGINORE_ACTIVITY_ITEM.value());
        }
        //满足数量且限定活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),2)&&!editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_QUANTITY_NOT_IGINORE_ACTIVITY_ITEM.value());
            activity.setOrderQuantity(editSubmitGiftActivityInfo.getQuantity());
        }
        //限制活动参与人数
        if (editSubmitGiftActivityInfo.getLimitQuantity()!=0){
            activity.setActivityQuantity(editSubmitGiftActivityInfo.getLimitQuantity());
            activity.setAlreadyActivityQuantity(0);
            activity.setQuantityRule(PoushengGiftQuantityRule.NO_LIMIT_PARTICIPANTS.value());
        }else{
            activity.setQuantityRule(PoushengGiftQuantityRule.LIMIT_PARTICIPANTS.value());
        }
        activity.setActivityEndAt(editSubmitGiftActivityInfo.getActivityStartDate());
        activity.setActivityEndAt(editSubmitGiftActivityInfo.getActivityEndDate());
        activity.setStatus(PoushengGiftActivityStatus.WAIT_PUBLISH.getValue());
        int totalPrice=0;
        for (GiftItem giftItem:giftItems){
            SkuTemplate skuTemplate = this.getSkuTemplate(giftItem.getSkuCode());
            //吊牌价
            Integer originSkuPrice = this.getOriginSkuPrice(skuTemplate);
            totalPrice = totalPrice+originSkuPrice;
        }
        activity.setTotalPrice(totalPrice);

        Map<String ,String> extraMap = Maps.newHashMap();
        extraMap.put(TradeConstants.ACTIVITY_SHOP,mapper.toJson(activityShops));
        extraMap.put(TradeConstants.ACTIVITY_ITEM,mapper.toJson(activityItems));
        extraMap.put(TradeConstants.GIFT_ITEM,mapper.toJson(giftItems));
        activity.setExtra(extraMap);
        Response<Long> r = poushengGiftActivityWriteService.create(activity);
        if (!r.isSuccess()){
            log.error("create pousheng gift activity failed, domain is {},caused by {}",activity,r.getError());
            throw new JsonResponseException("create.gift.activity.failed");
        }
        return r.getResult();
    }

    /**
     * 查询skuTemplate
     * @param skuCode
     * @return
     */
    public SkuTemplate getSkuTemplate(String skuCode){
        Response<List<SkuTemplate>> r = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if (!r.isSuccess()||r.getResult().size()==0){
            log.error("find skuTemplate failed,skuCode is {},caused by {}",skuCode,r.getError());
            throw new JsonResponseException("find.skuTemplate.failed");
        }
        return r.getResult().get(0);
    }

    /**
     * 获取吊牌价
     * @param skuTemplate
     * @return
     */
    public Integer getOriginSkuPrice(SkuTemplate skuTemplate){
        Map<String, Integer> priceMap =  skuTemplate.getExtraPrice();
        return priceMap.get("originPrice")==null?0:priceMap.get("originPrice");
    }
}
