package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import com.pousheng.middle.order.service.PoushengGiftActivityReadService;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.PoushengGiftActivityReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.POUSHENG_GIFT_ACTIVITY)
public class GiftActivityReader {

    @Autowired
    private PoushengGiftActivityReadService poushengGiftActivityReadService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private PoushengGiftActivityReadLogic poushengGiftActivityReadLogic;
    /**
     * 宝胜赠品活动列表
     * @param criteria
     */
    @RequestMapping(value = "/api/gift/activity/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<PoushengGiftActivityPagingInfo>> findBy(PoushengGiftActivityCriteria criteria){
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-GIFT-ACTIVITY-PAGING-START param: criteria [{}]",criteriaStr);
        }
        Response<Paging<PoushengGiftActivity>> r =  poushengGiftActivityReadService.paging(criteria);
        if (!r.isSuccess()){
            log.error("find pousheng gift activity paging failed,criteria is {},caused by {}",criteria,r.getError());
            throw new JsonResponseException("find.gift.activity.paging.failed");
        }
        Flow flow = flowPicker.pickGiftActivity();
        List<PoushengGiftActivity> poushengGiftActivities = r.getResult().getData();
        Paging<PoushengGiftActivityPagingInfo> pagingInfoPaging = Paging.empty();
        List<PoushengGiftActivityPagingInfo> pagingInfos = Lists.newArrayListWithCapacity(poushengGiftActivities.size());
        poushengGiftActivities.forEach(poushengGiftActivity -> {
            PoushengGiftActivityPagingInfo poushengGiftActivityPagingInfo = new PoushengGiftActivityPagingInfo();
            poushengGiftActivityPagingInfo.setPoushengGiftActivity(poushengGiftActivity);
            poushengGiftActivityPagingInfo.setGiftOperations(flow.availableOperations(poushengGiftActivity.getStatus()).stream().collect(Collectors.toSet()));
            pagingInfos.add(poushengGiftActivityPagingInfo);
        });
        pagingInfoPaging.setData(pagingInfos);
        pagingInfoPaging.setTotal(r.getResult().getTotal());
        if(log.isDebugEnabled()){
            log.debug("API-GIFT-ACTIVITY-PAGING-END param: criteria [{}] ,resp: [{}]",criteriaStr,JsonMapper.nonEmptyMapper().toJson(pagingInfoPaging));
        }
        return Response.ok(pagingInfoPaging);
    }

    /**
     * 单个活动查询
     * @param id
     * @return
     */
    @RequestMapping(value ="/api/gift/activity/{id}/info",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<PoushengGiftActivityInfo> findById(@PathVariable("id") Long id){
        if(log.isDebugEnabled()){
            log.debug("API-GIFT-ACTIVITY-INFO-START param: id [{}]",id);
        }
        Response<PoushengGiftActivity> r = poushengGiftActivityReadService.findById(id);
        if (!r.isSuccess()){
            log.error("find pousheng gift activity by id failed,id is {},caused by {}",id,r.getError());
            throw new JsonResponseException("find.single.gift.activity.failed");
        }
        PoushengGiftActivity activity = r.getResult();
        List<GiftItem> giftItems = poushengGiftActivityReadLogic.getGiftItem(activity);
        List<ActivityItem> activityItems = poushengGiftActivityReadLogic.getActivityItem(activity);
        List<ActivityShop> activityShops = poushengGiftActivityReadLogic.getActivityShopList(activity);
        PoushengGiftActivityInfo poushengGiftActivityInfo = new PoushengGiftActivityInfo();
        poushengGiftActivityInfo.setPoushengGiftActivity(activity);
        poushengGiftActivityInfo.setActivityItems(activityItems);
        poushengGiftActivityInfo.setGiftItems(giftItems);
        poushengGiftActivityInfo.setActivityShops(activityShops);
        if(log.isDebugEnabled()){
            log.debug("API-GIFT-ACTIVITY-INFO-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(poushengGiftActivityInfo));
        }
        return Response.ok(poushengGiftActivityInfo);
    }

    /**
     * 分页查询活动中的活动商品
     * @param id 活动id
     * @return
     */
    @RequestMapping(value ="/api/pousheng/activity/item/{id}/info",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<ActivityItem>> PagingActivityItemByActivityId(@PathVariable("id") Long id){
        if(log.isDebugEnabled()){
            log.debug("API-POUSHENG-ACTIVITY-ITEM-START param: id [{}]",id);
        }
        PoushengGiftActivityCriteria criteria = new PoushengGiftActivityCriteria();
        criteria.setId(id);
        Response<Paging<ActivityItem>> r = poushengGiftActivityReadLogic.pagingActivityItems(criteria);
        if (!r.isSuccess()){
            log.error("find pousheng activity item info failed,activity id is {},caused by {}",id,r.getError());
            throw new JsonResponseException("find.pousehng.activity.item.failed");
        }
        if(log.isDebugEnabled()){
            log.debug("API-POUSHENG-ACTIVITY-ITEM-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(r));
        }
        return r;
    }
}
