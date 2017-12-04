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
import com.pousheng.middle.order.service.PoushengGiftActivityReadService;
import com.pousheng.middle.order.service.PoushengGiftActivityWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    @RpcConsumer
    private PoushengGiftActivityWriteService poushengGiftActivityWriteService;

    @RpcConsumer
    private PoushengGiftActivityReadService poushengGiftActivityReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private MiddleOrderFlowPicker flowPicker;


    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    /**
     * 创建活动规则
     * @param editSubmitGiftActivityInfo
     * @return
     */
    public long createGiftActivity(EditSubmitGiftActivityInfo editSubmitGiftActivityInfo)
    {

        PoushengGiftActivity activity = new PoushengGiftActivity();
        activity.setName(editSubmitGiftActivityInfo.getName());
        //满足金额且不限活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),1)&&editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_FEE_IGINORE_ACTIVITY_ITEM.value());
            activity.setOrderFee(editSubmitGiftActivityInfo.getFee());
        }
        //满足金额且限定活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),1)&&!editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_FEE_NOT_IGINORE_ACTIVITY_ITEM.value());
            activity.setOrderFee(editSubmitGiftActivityInfo.getFee());
        }
        //满足数量且不限活动商品
        if (Objects.equals(editSubmitGiftActivityInfo.getActivityType(),2)&&editSubmitGiftActivityInfo.getIsNoLimitItem()){
            activity.setOrderRule(PoushengGiftOrderRule.SATIFIED_QUANTITY_IGINORE_ACTIVITY_ITEM.value());
            activity.setOrderQuantity(editSubmitGiftActivityInfo.getQuantity());
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
            activity.setQuantityRule(PoushengGiftQuantityRule.LIMIT_PARTICIPANTS.value());
        }else{
            activity.setQuantityRule(PoushengGiftQuantityRule.NO_LIMIT_PARTICIPANTS.value());
        }
        activity.setActivityStartAt(editSubmitGiftActivityInfo.getActivityStartDate());
        activity.setActivityEndAt(editSubmitGiftActivityInfo.getActivityEndDate());
        activity.setStatus(PoushengGiftActivityStatus.WAIT_PUBLISH.getValue());

        //活动店铺
        List<ActivityShop> activityShops = editSubmitGiftActivityInfo.getActivityShops()==null?Lists.newArrayList():editSubmitGiftActivityInfo.getActivityShops();
        //赠品
        List<GiftItem> giftItems = editSubmitGiftActivityInfo.getGiftItems()==null?Lists.newArrayList(): editSubmitGiftActivityInfo.getGiftItems();
        //活动商品
        List<ActivityItem> activityItems = editSubmitGiftActivityInfo.getActivityItems()==null?Lists.newArrayList():editSubmitGiftActivityInfo.getActivityItems();

        int totalPrice=0;
        for (GiftItem giftItem:giftItems){
            SkuTemplate skuTemplate = this.getSkuTemplate(giftItem.getSkuCode());
            //如果前端传入的price为空，则显示吊牌价
            Integer originSkuPrice = giftItem.getPrice()!=null?giftItem.getPrice():this.getOriginSkuPrice(skuTemplate);
            totalPrice = totalPrice+originSkuPrice;
            giftItem.setSpuId(skuTemplate.getSpuId());
            giftItem.setMaterialCode(this.getMaterialCode(skuTemplate));
            giftItem.setAttrs(skuTemplate.getAttrs());
            giftItem.setSkuTemplateId(skuTemplate.getId());
        }
        for (ActivityItem activityItem:activityItems){
            SkuTemplate skuTemplate = this.getSkuTemplate(activityItem.getSkuCode());
            activityItem.setSpuId(skuTemplate.getSpuId());
            activityItem.setMaterialCode(this.getMaterialCode(skuTemplate));
            activityItem.setAttrs(skuTemplate.getAttrs());
            activityItem.setSkuTemplateId(skuTemplate.getId());
        }
        //获取活动的赠品的总的金额
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
     *
     * @param editSubmitGiftActivityInfo
     * @return
     */
    public Boolean updateGiftActivity(EditSubmitGiftActivityInfo editSubmitGiftActivityInfo)
    {
        //判断活动是否存在
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(editSubmitGiftActivityInfo.getId());
        if (!r.isSuccess()||Objects.isNull(r.getResult())){
            log.error("find pousheng gift activity faile,id is {},caused by {}",editSubmitGiftActivityInfo.getId(),r.getError());
            throw new JsonResponseException("find.single.poushengGiftActivity.failed");
        }
        //活动店铺
        List<ActivityShop> activityShops = editSubmitGiftActivityInfo.getActivityShops()==null?Lists.newArrayList():editSubmitGiftActivityInfo.getActivityShops();
        //赠品
        List<GiftItem> giftItems = editSubmitGiftActivityInfo.getGiftItems()==null?Lists.newArrayList(): editSubmitGiftActivityInfo.getGiftItems();
        //活动商品
        List<ActivityItem> activityItems = editSubmitGiftActivityInfo.getActivityItems()==null?Lists.newArrayList():editSubmitGiftActivityInfo.getActivityItems();


        PoushengGiftActivity activity = r.getResult();
        //活动名称
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
            activity.setQuantityRule(PoushengGiftQuantityRule.NO_LIMIT_PARTICIPANTS.value());
        }else{
            activity.setQuantityRule(PoushengGiftQuantityRule.LIMIT_PARTICIPANTS.value());
        }
        activity.setActivityEndAt(editSubmitGiftActivityInfo.getActivityStartDate());
        activity.setActivityEndAt(editSubmitGiftActivityInfo.getActivityEndDate());
        activity.setStatus(editSubmitGiftActivityInfo.getStatus());
        int totalPrice=0;
        for (GiftItem giftItem:giftItems){
            SkuTemplate skuTemplate = this.getSkuTemplate(giftItem.getSkuCode());
            //吊牌价
            Integer originSkuPrice = this.getOriginSkuPrice(skuTemplate);
            totalPrice = totalPrice+originSkuPrice;
            giftItem.setSpuId(skuTemplate.getSpuId());
            giftItem.setMaterialCode(this.getMaterialCode(skuTemplate));
            giftItem.setAttrs(skuTemplate.getAttrs());
        }

        for (ActivityItem activityItem:activityItems){
            SkuTemplate skuTemplate = this.getSkuTemplate(activityItem.getSkuCode());
            activityItem.setSpuId(skuTemplate.getSpuId());
            activityItem.setMaterialCode(this.getMaterialCode(skuTemplate));
            activityItem.setAttrs(skuTemplate.getAttrs());
        }
        activity.setTotalPrice(totalPrice);
        Map<String ,String> extraMap = Maps.newHashMap();
        extraMap.put(TradeConstants.ACTIVITY_SHOP,mapper.toJson(activityShops));
        extraMap.put(TradeConstants.ACTIVITY_ITEM,mapper.toJson(activityItems));
        extraMap.put(TradeConstants.GIFT_ITEM,mapper.toJson(giftItems));
        activity.setExtra(extraMap);
        Response<Boolean> rU = poushengGiftActivityWriteService.update(activity);
        if (!rU.isSuccess()){
            log.error("create pousheng gift activity failed, domain is {},caused by {}",activity,r.getError());
            throw new JsonResponseException("create.gift.activity.failed");
        }
        return rU.getResult();
    }


    /**
     * 更新活动表的状态
     * @param orderOperation
     * @return
     */
    public Response<Boolean> updatePoushengGiftActivityStatus(Long id, OrderOperation orderOperation){
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(id);
        if (!r.isSuccess()||Objects.isNull(r.getResult())){
            log.error("find pousheng gift activity faile,id is {},caused by {}",id,r.getError());
            throw new JsonResponseException("find.single.poushengGiftActivity.failed");
        }
        PoushengGiftActivity activity = r.getResult();

        Flow flow = flowPicker.pickGiftActivity();
        if (!flow.operationAllowed(activity.getStatus(), orderOperation)) {
            log.error("poushengGiftActivity(id:{}) current status:{} not allow operation:{}", activity.getId(), activity.getStatus(), orderOperation.getText());
            throw new JsonResponseException("poushengGiftActivity.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(activity.getStatus(), orderOperation);

        PoushengGiftActivity newActivity= new PoushengGiftActivity();
        newActivity.setId(activity.getId());
        newActivity.setStatus(targetStatus);
        Response<Boolean> updateRes = poushengGiftActivityWriteService.update(newActivity);
        if (!updateRes.isSuccess()) {
            log.error("update poushengGiftActivity(id:{}) status to:{} fail,error:{}", activity.getId(), updateRes.getError());
            throw new JsonResponseException("update.poushengGiftActivity.failed");
        }
        return Response.ok(Boolean.TRUE);
    }



    /**
     * 更新参与活动的人数
     * @return
     */
    public Response<Boolean> updatePoushengGiftActivityParticipants(Long id){
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(id);
        if (!r.isSuccess()||Objects.isNull(r.getResult())){
            log.error("find pousheng gift activity faile,id is {},caused by {}",id,r.getError());
            throw new JsonResponseException("find.single.poushengGiftActivity.failed");
        }
        PoushengGiftActivity activity = r.getResult();
        PoushengGiftActivity newActivity= new PoushengGiftActivity();
        newActivity.setId(activity.getId());
        newActivity.setAlreadyActivityQuantity(activity.getAlreadyActivityQuantity()+1);
        Response<Boolean> updateRes = poushengGiftActivityWriteService.update(newActivity);
        if (!updateRes.isSuccess()) {
            log.error("update poushengGiftActivity(id:{}) status to:{} fail,error:{}", activity.getId(), updateRes.getError());
            throw new JsonResponseException("update.poushengGiftActivity.failed");
        }
        return Response.ok(Boolean.TRUE);
    }
    /**
     * 查询skuTemplate
     * @param skuCode
     * @return
     */
    private SkuTemplate getSkuTemplate(String skuCode){
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
    private Integer getOriginSkuPrice(SkuTemplate skuTemplate){
        Map<String, Integer> priceMap =  skuTemplate.getExtraPrice();
        return priceMap.get("originPrice")==null?0:priceMap.get("originPrice");
    }


    private  String getMaterialCode(SkuTemplate skuTemplate){
        Map<String, String> extraMap =  skuTemplate.getExtra();
        return StringUtils.isEmpty(extraMap.get("materialCode"))?"":extraMap.get("materialCode");
    }
}
