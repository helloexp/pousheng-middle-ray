package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.order.service.MiddleRefundReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.RefundReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/7/13
 * Author: yangzefeng
 */
@Component
@Slf4j
public class RefundReadLogic {

    @Autowired
    private MiddleRefundReadService middleRefundReadService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    public Response<Paging<RefundPaging>> refundPaging(MiddleRefundCriteria criteria) {
        Response<Paging<Refund>> refundsR = middleRefundReadService.paging(criteria);
        if (!refundsR.isSuccess()) {
            log.error("paging refund by criteria:{} fail,error:{}",criteria,refundsR.getError());
            return Response.fail(refundsR.getError());
        }

        Paging<Refund> refundPaging = refundsR.getResult();
        List<Refund> refunds = refundPaging.getData();
        Response<Map<Long,OrderRefund>> groupByRefundIdMapRes = groupOrderRerundByRefundId(refunds);
        if(!groupByRefundIdMapRes.isSuccess()){
            return Response.fail(groupByRefundIdMapRes.getError());
        }

        Map<Long,OrderRefund>  groupByRefundIdMap = groupByRefundIdMapRes.getResult();
        Flow flow = flowPicker.pickAfterSales();
        Paging<RefundPaging> paging = new Paging<>();
        paging.setTotal(refundPaging.getTotal());
        List<RefundPaging> refundLists = Lists.newArrayListWithCapacity(refunds.size());

        for (Refund refund : refunds) {
            RefundPaging rp  = new RefundPaging();
            rp.setRefund(refund);
            rp.setOrderRefund(groupByRefundIdMap.get(refund.getId()));
            Set<OrderOperation> operations = flow.availableOperations(refund.getStatus());
            rp.setOperations(operations);
            refundLists.add(rp);
        }
        paging.setData(refundLists);
        return Response.ok(paging);
    }

    public Response<Map<Long,OrderRefund>> groupOrderRerundByRefundId(List<Refund> refunds){

        if(CollectionUtils.isEmpty(refunds)){
            return Response.ok(Maps.newHashMap());
        }
        List<Long> refundIds = Lists.transform(refunds, new Function<Refund, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Refund refund) {
                return refund.getId();
            }
        });
        Response<List<OrderRefund>> listRes = middleRefundReadService.findOrderRefundByRefundIds(refundIds);
        if(!listRes.isSuccess()){
            log.error("find order refund by refund ids:{} fail,error:{}",refundIds,listRes.getError());
            return Response.fail(listRes.getError());
        }
        List<OrderRefund> orderRefunds = listRes.getResult();
        Map<Long,OrderRefund> groupByRefundIdMap = orderRefunds.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(OrderRefund::getRefundId, it -> it));
        return Response.ok(groupByRefundIdMap);
    }

    public Refund findRefundById(Long refundId){
        Response<Refund> refundRes = refundReadService.findById(refundId);
        if(!refundRes.isSuccess()){
            log.error("find refund by id:{} fail,error:{}",refundId,refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }


    public List<Refund> findRefundByIds(List<Long> refundIds){
        Response<List<Refund>> refundRes = refundReadService.findByIds(refundIds);
        if(!refundRes.isSuccess()){
            log.error("find refund by ids:{} fail,error:{}",refundIds,refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        return refundRes.getResult();
    }


    public OrderRefund findOrderRefundByRefundId(Long refundId){
        Response<List<OrderRefund>> listRes = refundReadService.findOrderIdsByRefundId(refundId);
        if(!listRes.isSuccess()){
            log.error("find order refund by refund id:{} fail,error:{}",refundId,listRes.getError());
            throw new JsonResponseException(listRes.getError());
        }

        List<OrderRefund> orderRefunds = listRes.getResult();
        if(CollectionUtils.isEmpty(orderRefunds)){
            log.error("not find order refund by refund id:{}",refundId);
            throw new JsonResponseException("order.refund.not.exist");
        }

        return orderRefunds.get(0);

    }

    public List<RefundItem> findRefundItems(Refund refund){
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.warn("refund(id:{}) extra field is null",refund.getId());
            return Lists.newArrayList(new RefundItem());
        }
        if(!extraMap.containsKey(TradeConstants.REFUND_ITEM_INFO)){
            log.warn("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_ITEM_INFO);
            return Lists.newArrayList(new RefundItem());
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_ITEM_INFO),mapper.createCollectionType(List.class,RefundItem.class));
    }


    public List<RefundItem> findRefundChangeItems(Refund refund){
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("refund(id:{}) extra field is null",refund.getId());
            throw new JsonResponseException("refund.extra.is.empty");
        }
        if(!extraMap.containsKey(TradeConstants.REFUND_CHANGE_ITEM_INFO)){
            log.error("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_CHANGE_ITEM_INFO);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_CHANGE_ITEM_INFO),mapper.createCollectionType(List.class,RefundItem.class));
    }



    public RefundSource findRefundSource(Refund refund){
        Map<String,String> tagMap = refund.getTags();
        if(CollectionUtils.isEmpty(tagMap)){
            log.error("refund(id:{}) tag_Json field is null",refund.getId());
            throw new JsonResponseException("refund.tag.is.empty");
        }
        if(!tagMap.containsKey(TradeConstants.REFUND_SOURCE)){
            log.error("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_SOURCE);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        return RefundSource.from(Integer.valueOf(tagMap.get(TradeConstants.REFUND_SOURCE)));
    }




    public RefundExtra findRefundExtra(Refund refund){
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("refund(id:{}) extra field is null",refund.getId());
            return new RefundExtra();
        }
        if(!extraMap.containsKey(TradeConstants.REFUND_EXTRA_INFO)){
            log.error("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.REFUND_EXTRA_INFO);
            return new RefundExtra();
        }

        return mapper.fromJson(extraMap.get(TradeConstants.REFUND_EXTRA_INFO),RefundExtra.class);
    }

    public MiddleChangeReceiveInfo findMiddleChangeReceiveInfo(Refund refund){
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("refund(id:{}) extra field is null",refund.getId());
            return new MiddleChangeReceiveInfo();
        }
        if(!extraMap.containsKey(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO)){
            log.error("refund(id:{}) extra map not contain key:{}",refund.getId(),TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO);
            return new MiddleChangeReceiveInfo();
        }
        return mapper.fromJson(extraMap.get(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO),MiddleChangeReceiveInfo.class);
    }

    /**
     * 判断收货完成的售后换货单是否可以取消发货
     * @param refund 售后单
     * @return 可以取消发货(true),不可以取消发货(false)
     */
    public boolean isAfterSaleCanCancelShip(Refund refund){
        Flow afterSaleFlow = flowPicker.pickAfterSales();
        Integer sourceStatus = refund.getStatus();
        return afterSaleFlow.operationAllowed(sourceStatus, MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
    }

    /**
     * 返回最多可退金额
     * @param orderId 订单id
     * @param refundId 退货单id
     * @param shipmentId 发货单id
     * @param skuCode 条码
     * @param applyQuantity
     * @return
     */
    public int getAlreadyRefundFee(Long orderId,Long refundId,Long shipmentId,String skuCode,Integer applyQuantity){
        Response<List<Refund>> rltRes = refundReadService.findByOrderIdAndOrderLevel(orderId, OrderLevel.SHOP);
        if (!rltRes.isSuccess()){
            log.error("find Refund failed,order id is ({}),caused by {}",orderId,rltRes.getError());
            throw new JsonResponseException(rltRes.getError());
        }
        //获取不是订单号关联的其他售后单，过滤掉换货的，已经取消的，当前传入的需要计算的售后单
        List<Refund> refunds = rltRes.getResult().stream().filter(Objects::nonNull)
                .filter(refund->!Objects.equals(refund.getId(),refundId))
                .filter(refund->!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value()))
                .filter(refund -> !Objects.equals(refund.getStatus(), MiddleRefundStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        Long alreadyRefundFee = 0L;
        for (Refund refund:refunds){
            List<RefundItem> refundItems = this.findRefundItems(refund);
            List<String> skuCodes = refundItems.stream().map(RefundItem::getSkuCode).collect(Collectors.toList());
            if (skuCodes.contains(skuCode)){
                alreadyRefundFee+= refund.getFee();
            }
        }
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        Optional<ShipmentItem> shipmentItemOpt = shipmentItems.stream().filter(Objects::nonNull).
                filter(shipmentItem -> Objects.equals(shipmentItem.getSkuCode(),skuCode)).findFirst();
        ShipmentItem shipmentItem = shipmentItemOpt.get();
        //获取商品总净价
        Integer cleanFee = (shipmentItem.getCleanPrice()==null?0:shipmentItem.getCleanPrice())*applyQuantity;
        //未退款金额
        Integer unReturnedFee = Math.toIntExact((shipmentItem.getCleanFee() == null ? 0 : shipmentItem.getCleanFee()) - alreadyRefundFee);
        return cleanFee>unReturnedFee?unReturnedFee:cleanFee;
    }
}
