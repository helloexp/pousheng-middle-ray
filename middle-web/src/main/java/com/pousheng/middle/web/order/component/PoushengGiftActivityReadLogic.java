package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ActivityItem;
import com.pousheng.middle.order.dto.ActivityShop;
import com.pousheng.middle.order.dto.GiftItem;
import com.pousheng.middle.order.dto.PoushengGiftActivityCriteria;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityReadService;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 * @author tony
 */
@Component
@Slf4j
public class PoushengGiftActivityReadLogic {
    @Autowired
    private PoushengGiftActivityReadService poushengGiftActivityReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final Ordering<ActivityItem> bySpuId = Ordering.natural().onResultOf(new Function<ActivityItem, Long>() {
        @Override
        public Long apply(ActivityItem input) {
            return input.getSpuId();
        }
    });
    /**
     * 获取赠品活动信息
     * @param activityId
     * @return
     */
    public PoushengGiftActivity findPoushengGiftActivityById(Long activityId){
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(activityId);
        if(!r.isSuccess()){
            log.error("find poushengGiftActivity by id:{} fail,error:{}",activityId,r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
    /**
     * 获取活动商品信息列表
     * @param poushengGiftActivity 赠品活动
     * @return
     */
    public List<ActivityItem> getActivityItem(PoushengGiftActivity poushengGiftActivity){
        Map<String,String> extraMap = poushengGiftActivity.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("poushengGiftActivity(id:{}) extra field is null",poushengGiftActivity.getId());
            throw new JsonResponseException("poushengGiftActivity.extra.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.ACTIVITY_ITEM)){
            log.error("poushengGiftActivity(id:{}) extra not contain key:{}",poushengGiftActivity.getId(),TradeConstants.ACTIVITY_ITEM);
            throw new JsonResponseException("poushengGiftActivity.item.info.null");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.ACTIVITY_ITEM),mapper.createCollectionType(List.class,ActivityItem.class));
    }


    /**
     * 获取活动商品信息列表
     * @param poushengGiftActivity 赠品活动
     * @return
     */
    public List<ActivityShop> getActivityShopList(PoushengGiftActivity poushengGiftActivity){
        Map<String,String> extraMap = poushengGiftActivity.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("poushengGiftActivity(id:{}) extra field is null",poushengGiftActivity.getId());
            throw new JsonResponseException("poushengGiftActivity.extra.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.ACTIVITY_SHOP)){
            log.error("poushengGiftActivity(id:{}) extra not contain key:{}",poushengGiftActivity.getId(),TradeConstants.ACTIVITY_SHOP);
            throw new JsonResponseException("poushengGiftActivity.item.info.null");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.ACTIVITY_SHOP),mapper.createCollectionType(List.class,ActivityShop.class));
    }



    /**
     * 获取赠品商品信息列表
     * @param poushengGiftActivity 赠品活动
     * @return
     */
    public List<GiftItem> getGiftItem(PoushengGiftActivity poushengGiftActivity){
        Map<String,String> extraMap = poushengGiftActivity.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("poushengGiftActivity(id:{}) extra field is null",poushengGiftActivity.getId());
            throw new JsonResponseException("poushengGiftActivity.extra.is.null");
        }
        if(!extraMap.containsKey(TradeConstants.GIFT_ITEM)){
            log.error("poushengGiftActivity(id:{}) extra not contain key:{}",poushengGiftActivity.getId(),TradeConstants.GIFT_ITEM);
            throw new JsonResponseException("poushengGiftActivity.gift.info.null");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.GIFT_ITEM),mapper.createCollectionType(List.class,GiftItem.class));
    }


    /**
     * 获取某些状态下的活动
     * @param status
     * @return
     */
    public List<PoushengGiftActivity> findByStatus(Integer ...status){
        PoushengGiftActivityCriteria criteria = new PoushengGiftActivityCriteria();
        criteria.setStatuses(Lists.newArrayList(status));
        List<PoushengGiftActivity> activities = Lists.newArrayList();
        int pageNo = 1;
        boolean next = batchHandle(pageNo, 20,criteria ,activities);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, 20,criteria,activities);
        }

        return activities;
    }


    private boolean batchHandle(int pageNo, int size,PoushengGiftActivityCriteria criteria,List<PoushengGiftActivity> activities) {

        Response<Paging<PoushengGiftActivity>> pagingRes = poushengGiftActivityReadService.paging(criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging sku order fail,criteria:{},error:{}",criteria,pagingRes.getError());
            return Boolean.FALSE;
        }
        Paging<PoushengGiftActivity> paging = pagingRes.getResult();
        List<PoushengGiftActivity> result = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(result)) {
            return Boolean.FALSE;
        }
        activities.addAll(result);

        int current = result.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }

    /**
     * 分页查询活动商品
     * @param criteria
     * @return
     */
    public Response<Paging<ActivityItem>> pagingActivityItems(PoushengGiftActivityCriteria criteria){
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(criteria.getId());
        if (!r.isSuccess()){
            log.error("find pousheng gift activity by id failed,id is {},caused by {}",criteria.getId(),r.getError());
            throw new JsonResponseException("find.single.gift.activity.failed");
        }
        PoushengGiftActivity activity = r.getResult();
        List<ActivityItem> activityItems = bySpuId.sortedCopy(this.getActivityItem(activity));

        Long total = Long.valueOf(activityItems.size());
        if (total <= 0){
            return Response.ok(new Paging<ActivityItem>(0L, Collections.<ActivityItem>emptyList()));
        }
        Integer limit = criteria.getLimit();//每页记录数
        Integer offset = criteria.getOffset();//偏移量
        return null;
    }
}
