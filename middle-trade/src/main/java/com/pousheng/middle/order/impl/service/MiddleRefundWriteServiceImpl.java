package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleChangeReceiveInfo;
import com.pousheng.middle.order.impl.manager.MiddleRefundManager;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.impl.dao.RefundDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/6/28
 */
@Slf4j
@Service
public class MiddleRefundWriteServiceImpl implements MiddleRefundWriteService{

    @Autowired
    private MiddleRefundManager middleRefundManager;
    @Autowired
    private ShopOrderDao orderDao;
    @Autowired
    private RefundDao refundDao;


    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();



    @Override
    public Response<Long> create(Refund refund, List<Long> orderIds, OrderLevel orderLevel) {

        try {
            //默认状态为申请退款
            refund.setStatus(MoreObjects.firstNonNull(refund.getStatus(), OrderStatus.REFUND_APPLY.getValue()));
            List<OrderRefund> orderRefunds = Lists.newArrayListWithCapacity(orderIds.size());
            for (Long orderId : orderIds) {
                OrderRefund orderRefund = new OrderRefund();
                orderRefund.setOrderId(orderId);
                ShopOrder shopOrder = orderDao.findById(orderId);
                orderRefund.setOrderCode(shopOrder.getOrderCode());
                orderRefund.setOrderLevel(orderLevel);
                orderRefund.setStatus(refund.getStatus());
                orderRefunds.add(orderRefund);
            }
            Long refundId = middleRefundManager.create(refund, orderRefunds);
            return Response.ok(refundId);
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", refund, Throwables.getStackTraceAsString(e));
            return Response.fail("refund.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateReceiveInfos(long refundId, MiddleChangeReceiveInfo middleChangeReceiveInfo) {
        Refund refund = refundDao.findById(refundId);
        Map<String,String> extraMap = refund.getExtra()!=null?refund.getExtra():Maps.newHashMap();
        MiddleChangeReceiveInfo originReceiverInfo = null;
        if(!extraMap.containsKey(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO)){
            log.warn("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO);
            originReceiverInfo = new MiddleChangeReceiveInfo();
        }else{
            originReceiverInfo =  mapper.fromJson(extraMap.get(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO),MiddleChangeReceiveInfo.class);
        }

        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getReceiveUserName())){
            originReceiverInfo.setReceiveUserName(middleChangeReceiveInfo.getReceiveUserName());
        }
        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getMobile())){
            originReceiverInfo.setMobile(middleChangeReceiveInfo.getMobile());
        }
        if(!StringUtils.isEmpty(middleChangeReceiveInfo.getProvince())){
            originReceiverInfo.setProvince(middleChangeReceiveInfo.getProvince());
        }
        if (middleChangeReceiveInfo.getProvinceId()!=null){
            originReceiverInfo.setProvinceId(middleChangeReceiveInfo.getProvinceId());
        }
        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getCity())){
            originReceiverInfo.setCity(middleChangeReceiveInfo.getCity());
        }
        if (middleChangeReceiveInfo.getCityId()!= null){
            originReceiverInfo.setCityId(middleChangeReceiveInfo.getCityId());
        }
        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getRegion())){
            originReceiverInfo.setRegion(middleChangeReceiveInfo.getRegion());
        }
        if (middleChangeReceiveInfo.getRegionId()!=null){
            originReceiverInfo.setRegionId(middleChangeReceiveInfo.getRegionId());
        }
        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getDetail())){
            originReceiverInfo.setDetail(middleChangeReceiveInfo.getDetail());
        }
        if (!StringUtils.isEmpty(middleChangeReceiveInfo.getPostcode())){
            originReceiverInfo.setPostcode(middleChangeReceiveInfo.getPostcode());
        }

        extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO, mapper.toJson(originReceiverInfo));
        refund.setExtra(extraMap);
        Boolean result = refundDao.update(refund);
        return Response.ok(result);
    }



}
