package com.pousheng.middle.order.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.impl.manager.MiddleRefundManager;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.order.impl.dao.RefundDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
    public Response<Boolean> updateReceiveInfos(long refundId,String buyerName, ReceiverInfo receiverInfo) {
        Refund refund = refundDao.findById(refundId);
        Map<String,String> extraMap = refund.getExtra()!=null?refund.getExtra():Maps.newHashMap();
        RefundExtra refundExtra = null;
        if(!extraMap.containsKey(TradeConstants.REFUND_EXTRA_INFO)){
            log.warn("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_EXTRA_INFO);
            refundExtra = new RefundExtra();
        }else{
            refundExtra =  mapper.fromJson(extraMap.get(TradeConstants.REFUND_EXTRA_INFO),RefundExtra.class);
        }
        ReceiverInfo originReceiverInfo = null;
        if (refundExtra.getReceiverInfo()==null){
            originReceiverInfo = receiverInfo;
        }else{
            originReceiverInfo  = refundExtra.getReceiverInfo();
            if (!StringUtils.isEmpty(receiverInfo.getReceiveUserName())){
                originReceiverInfo.setReceiveUserName(receiverInfo.getReceiveUserName());
            }
            if (!StringUtils.isEmpty(receiverInfo.getMobile())){
                originReceiverInfo.setMobile(receiverInfo.getMobile());
            }
            if(!StringUtils.isEmpty(receiverInfo.getProvince())){
                originReceiverInfo.setProvince(receiverInfo.getProvince());
            }
            if (receiverInfo.getProvinceId()!=null){
                originReceiverInfo.setProvinceId(receiverInfo.getProvinceId());
            }
            if (!StringUtils.isEmpty(receiverInfo.getCity())){
                originReceiverInfo.setCity(receiverInfo.getCity());
            }
            if (receiverInfo.getCityId()!= null){
                originReceiverInfo.setCityId(receiverInfo.getCityId());
            }
            if (!StringUtils.isEmpty(receiverInfo.getRegion())){
                originReceiverInfo.setRegion(receiverInfo.getRegion());
            }
            if (receiverInfo.getRegionId()!=null){
                originReceiverInfo.setRegionId(receiverInfo.getRegionId());
            }
            if (!StringUtils.isEmpty(receiverInfo.getDetail())){
                originReceiverInfo.setDetail(receiverInfo.getDetail());
            }
            if (!StringUtils.isEmpty(receiverInfo.getPostcode())){
                originReceiverInfo.setPostcode(receiverInfo.getPostcode());
            }
        }
        refundExtra.setReceiverInfo(originReceiverInfo);
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        refund.setExtra(extraMap);
        refund.setBuyerName(buyerName);
        Boolean result = refundDao.update(refund);
        return Response.ok(result);
    }



}
