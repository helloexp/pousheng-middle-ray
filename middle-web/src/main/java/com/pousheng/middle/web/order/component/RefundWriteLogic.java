package com.pousheng.middle.web.order.component;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/7/1
 */
@Component
@Slf4j
public class RefundWriteLogic {


    @Autowired
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private SyncRefundLogic syncRefundLogic;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();



    /**
     * 更新换货商品处理数量
     * 判断是否商品已全部处理，如果是则更新状态为 WAIT_SHIP:待发货
     * @param skuCodeAndQuantity 商品编码及数量
     */
    public void updateSkuHandleNumber(Long refundId,Map<String,Integer> skuCodeAndQuantity){

        Refund refund = refundReadLogic.findRefundById(refundId);
        //换货商品
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //是否全部处理
        Boolean isAllHandle= Boolean.TRUE;
        //更新发货数量
        for (RefundItem refundItem : refundChangeItems) {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundItem.setAlreadyHandleNumber((refundItem.getAlreadyHandleNumber()==null?0:refundItem.getAlreadyHandleNumber()) + skuCodeAndQuantity.get(refundItem.getSkuCode()));
            }

            //如果存在未处理完成的
            if(!Objects.equals(refundItem.getApplyQuantity(),refundItem.getAlreadyHandleNumber())){
                isAllHandle = Boolean.FALSE;
            }else{
                //换货商品已经全部处理完,此时处于待发货状态,此时填入换货发货单创建时间
                refundExtra.setChangeShipmentAt(new Date());
            }
        }

        Refund update = new Refund();
        update.setId(refundId);
        Map<String,String> extrMap = refund.getExtra();
        extrMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundChangeItems));
        extrMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        update.setExtra(extrMap);

        Response<Boolean> updateRes = refundWriteService.update(update);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) fail,error:{}",refund,updateRes.getError());
        }

        if(isAllHandle){
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.CREATE_SHIPMENT.toOrderOperation());
            Response<Boolean> updateStatusRes = refundWriteService.updateStatus(refundId,targetStatus);
            if(!updateStatusRes.isSuccess()){
                log.error("update refund(id:{}) status to:{} fail,error:{}",refund,targetStatus,updateRes.getError());
            }
        }

    }

    /**
     * 更新丢件补发类型售后单已经处理数量
     * 判断是否商品已全部处理，如果是则更新状态为 WAIT_SHIP:待发货
     * @param skuCodeAndQuantity 商品编码及数量
     */
    public void updateSkuHandleNumberForLost(Long refundId,Map<String,Integer> skuCodeAndQuantity){

        Refund refund = refundReadLogic.findRefundById(refundId);
        //丢件补发商品
        List<RefundItem> refundLostItems = refundReadLogic.findRefundLostItems(refund);

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //是否全部处理
        Boolean isAllHandle= Boolean.TRUE;
        //更新发货数量
        for (RefundItem refundItem : refundLostItems) {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundItem.setAlreadyHandleNumber((refundItem.getAlreadyHandleNumber()==null?0:refundItem.getAlreadyHandleNumber()) + skuCodeAndQuantity.get(refundItem.getSkuCode()));
            }

            //如果存在未处理完成的
            if(!Objects.equals(refundItem.getApplyQuantity(),refundItem.getAlreadyHandleNumber())){
                isAllHandle = Boolean.FALSE;
            }else{
                //换货商品已经全部处理完,此时处于待发货状态,此时填入换货发货单创建时间
                refundExtra.setLostShipmentAt(new Date());
            }
        }

        Refund update = new Refund();
        update.setId(refundId);
        Map<String,String> extrMap = refund.getExtra();
        extrMap.put(TradeConstants.REFUND_LOST_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundLostItems));
        extrMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        update.setExtra(extrMap);

        Response<Boolean> updateRes = refundWriteService.update(update);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) fail,error:{}",refund,updateRes.getError());
        }

        if(isAllHandle){
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.LOST_CREATE_SHIP.toOrderOperation());
            Response<Boolean> updateStatusRes = refundWriteService.updateStatus(refundId,targetStatus);
            if(!updateStatusRes.isSuccess()){
                log.error("update refund(id:{}) status to:{} fail,error:{}",refund,targetStatus,updateRes.getError());
            }
        }

    }


    public Response<Boolean> updateStatus(Refund refund, OrderOperation orderOperation){

        Flow flow = flowPicker.pickAfterSales();
        if(!flow.operationAllowed(refund.getStatus(),orderOperation)){
            log.error("refund(id:{}) current status:{} not allow operation:{}",refund.getId(),refund.getStatus(),orderOperation.getText());
            return Response.fail("refund.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
        Response<Boolean> updateRes = refundWriteService.updateStatus(refund.getId(),targetStatus);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) status to:{} fail,error:{}",refund.getId(),updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok(Boolean.TRUE);

    }


    public Response<Boolean> update(Refund refund){

        Response<Boolean> updateRes = refundWriteService.update(refund);
        if(!updateRes.isSuccess()){
            log.error("update refund({}) status to:{} fail,error:{}",refund,updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok();

    }

    //删除逆向订单 限手动创建的
    public void deleteRefund(Refund refund){
        //判断类型
        RefundSource refundSource = refundReadLogic.findRefundSource(refund);
        if(Objects.equals(refundSource.value(),RefundSource.THIRD.value())){
            log.error("refund(id:{}) is third party refund  so cant not delete",refund.getId());
            throw new JsonResponseException("third.party.refund.can.not.delete");
        }

        //退货信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Map<String,RefundItem> skuCodeAndRefundItemMap = refundItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(RefundItem::getSkuCode, it -> it));


        //退货单所属发货单信息
        Shipment shipment = shipmentReadLogic.findShipmentById(refundExtra.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);



        //更新状态
        Response<Boolean> updateRes = this.updateStatus(refund,MiddleOrderEvent.DELETE.toOrderOperation());
        if(!updateRes.isSuccess()){
            log.error("delete refund(id:{}) fail,error:{}",refund.getId(),updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
        //回滚发货单的已退货数量
        shipmentItems.forEach(it -> {
            RefundItem refundItem = skuCodeAndRefundItemMap.get(it.getSkuCode());
            if (refundItem!=null)
            {
                it.setRefundQuantity(it.getRefundQuantity()-refundItem.getApplyQuantity());
            }

        });


        //更新发货单商品中的已退货数量
        Map<String,String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JsonMapper.nonDefaultMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(),shipmentExtraMap);
    }



    //创建售后单
    public Long createRefund(SubmitRefundInfo submitRefundInfo){
        //验证提交信息是否有效
        //订单是否有效
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //发货单是否有效
        Shipment shipment = shipmentReadLogic.findShipmentById(submitRefundInfo.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //申请数量是否有效
        List<RefundItem> refundItems = checkRefundQuantity(submitRefundInfo,shipmentItems);
        completeSkuAttributeInfo(refundItems);
        for (EditSubmitRefundItem editSubmitRefundItem:submitRefundInfo.getEditSubmitRefundItems()){
            updateShipmentItemRefundQuantity(editSubmitRefundItem.getRefundSkuCode(),editSubmitRefundItem.getRefundQuantity(),shipmentItems);
        }
        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setRefundAt(new Date());
        if(Objects.equals(submitRefundInfo.getOperationType(),1)){
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }else {
            refund.setStatus(MiddleRefundStatus.WAIT_SYNC_HK.getValue());
        }
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setFee(submitRefundInfo.getFee());
        refund.setRefundType(submitRefundInfo.getRefundType());

        Map<String,String> extraMap = Maps.newHashMap();

        RefundExtra refundExtra = new RefundExtra();

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        refundExtra.setShipmentId(shipment.getId());
        //完善仓库及物流信息
        completeWareHoseAndExpressInfo(submitRefundInfo.getRefundType(),refundExtra,submitRefundInfo);

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(refundItems));
        //完善换货信息
        completeChangeItemInfo(refundItems,submitRefundInfo.getRefundType(),submitRefundInfo,extraMap);
        //完善换货发货地址信息
        if (Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(),submitRefundInfo.getRefundType())){
            if (Objects.nonNull(submitRefundInfo.getMiddleChangeReceiveInfo())){
                extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO,mapper.toJson(submitRefundInfo.getMiddleChangeReceiveInfo()));
            }
        }

        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG,"0");
        refund.setExtra(extraMap);

        //打标
        Map<String,String> tagMap = Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.MANUAL.value()));
        refund.setTags(tagMap);




        //创建售后单
        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(submitRefundInfo.getOrderId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }

        //更新发货单商品中的已退货数量
        Map<String,String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JsonMapper.nonEmptyMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(),shipmentExtraMap);
        //如果是手工创建的售后单是点击的提交，直接同步恒康
        if(Objects.equals(submitRefundInfo.getOperationType(),2)){
            Refund newRefund = refundReadLogic.findRefundById(refund.getId());
            Response<Boolean> syncRes = syncRefundLogic.syncRefundToHk(newRefund);
            if (!syncRes.isSuccess()) {
                log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
            }
        }
        return rRefundRes.getResult();
    }

    /**
     * 完善售后单
     * @param refund 售后单
     * @param submitRefundInfo 前端传入的参数
     */
    public void completeHandle(Refund refund, EditSubmitRefundInfo submitRefundInfo){

        List<RefundItem> existRefundItems = refundReadLogic.findRefundItems(refund);
        List<RefundItem> currentRefundItems = existRefundItems;//当前编辑情况下的退货商品
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //发货单信息
        Shipment shipment = shipmentReadLogic.findShipmentById(refundExtra.getShipmentId()==null?submitRefundInfo.getShipmentId():refundExtra.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);


        Map<String,String> extraMap = refund.getExtra();

        //完善仓库及物流信息
        completeWareHoseAndExpressInfo(refund.getRefundType(),refundExtra,submitRefundInfo);
        //添加处理完成时间
        refundExtra.setHandleDoneAt(new Date());
        if (refundExtra.getShipmentId()==null){
            refundExtra.setShipmentId(submitRefundInfo.getShipmentId());
        }
        Refund updateRefund = new Refund();
        updateRefund.setId(refund.getId());
        updateRefund.setBuyerNote(submitRefundInfo.getBuyerNote());
        updateRefund.setFee(submitRefundInfo.getFee());

        //判断退货商品及数量是否有变化
        Boolean isRefundItemChanged = refundItemIsChanged(submitRefundInfo,existRefundItems);
        if(isRefundItemChanged){
            //申请数量是否有效
            currentRefundItems = checkRefundQuantity(submitRefundInfo,shipmentItems);
            //更新发货商品中的已退货数量
            updateShipmentItemRefundQuantityForEdit(shipmentItems,submitRefundInfo,existRefundItems);
            completeSkuAttributeInfo(currentRefundItems);
            extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(currentRefundItems));
        }

        //判断换货货商品及数量是否有变化
        Boolean isChangeItemChanged = changeItemIsChanged(refund,submitRefundInfo);
        if(isChangeItemChanged){
            //完善换货信息
            completeChangeItemInfo(currentRefundItems,refund.getRefundType(),submitRefundInfo,extraMap);
        }
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));

        //提交动作
        if(Objects.equals(submitRefundInfo.getOperationType(),2)){
            //更新售后单状态
            Response<Boolean> updateStatusRes = updateStatus(refund,MiddleOrderEvent.HANDLE.toOrderOperation());
            if(!updateStatusRes.isSuccess()){
                log.error("update refund(id:{}) status to:{} fail,error:{}",refund.getId(),updateStatusRes.getError());
                throw new JsonResponseException(updateStatusRes.getError());
            }
        }

        //更新退换货信息
        //退款总金额变化
        if(!Objects.equals(submitRefundInfo.getFee(),refund.getFee())){
            //如果只是改了退款金额，则要更新退货商品中的退款金额
            if(!isRefundItemChanged){
                //currentRefundItem.setFee(submitRefundInfo.getFee());
                //extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(currentRefundItems));

            }

            //如果只是改了退款金额,则更新换货商品中的退款金额
            if(!isChangeItemChanged&&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())){
                //RefundItem existChangeItem = refundReadLogic.findRefundChangeItems(refund).get(0);
                //existChangeItem.setFee(submitRefundInfo.getFee());
                //extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(existChangeItem)));
            }
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG,"0");

        updateRefund.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(updateRefund);
        if(!updateRes.isSuccess()){
            log.error("update refund:{} fail,error:{}",updateRefund,updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

        //如果退货商品信息变了则更新对应发货商品的退货数量
        if(isRefundItemChanged){
            //更新发货单商品中的已退货数量
            Map<String,String> shipmentExtraMap = shipment.getExtra();
            shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,mapper.toJson(shipmentItems));
            shipmentWiteLogic.updateExtra(shipment.getId(),shipmentExtraMap);
        }



    }

    /**
     * 判断退换货商品是否已经改变
     * @param editSubmitRefundInfo
     * @param existRefundItems 当前存在的sku
     * @return
     */
    private Boolean refundItemIsChanged(EditSubmitRefundInfo editSubmitRefundInfo,List<RefundItem> existRefundItems){
        Boolean isChanged = Boolean.FALSE;
        //传输进来的售后商品
        List<EditSubmitRefundItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitRefundItems();
        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitRefundItem::getRefundSkuCode).collect(Collectors.toList());
        //之前编辑的售后商品
        Map<String,Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        List<String> existSkuCodes = existRefundItems.stream().filter(Objects::nonNull).map(RefundItem::getSkuCode).collect(Collectors.toList());
        existRefundItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(),refundItem.getApplyQuantity());
        });
        //获取交集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes,existSkuCodes);
        //如果交集等于他们各自的数量，则表明没有skuCode变化
        if (commSkuCodes.size()==editSkuCodes.size()&&commSkuCodes.size()==existSkuCodes.size()){
            //如果不存在差集这比较两个商品之间是否存在金额变化的部分
            for (EditSubmitRefundItem editSubmitRefundItem: editSubmitRefundItems){
                Integer existQuantity = existSkuCodeAndQuantity.get(editSubmitRefundItem.getRefundSkuCode());
                if (!Objects.equals(editSubmitRefundItem.getRefundQuantity(),existQuantity)){
                    isChanged = Boolean.TRUE;
                    return isChanged;
                }
            }
        }else{
            //如果存在差集说明有商品变化
            isChanged = Boolean.TRUE;
            return isChanged;
        }
        return isChanged;
    }


    //换货商品是否改变
    private Boolean changeItemIsChanged(Refund refund,EditSubmitRefundInfo editSubmitRefundInfo){

        Boolean isChanged = Boolean.FALSE;

        //不为换货售后则直接返回false
        if(!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())){
            return Boolean.FALSE;
        }

        List<RefundItem> existChangeItems = refundReadLogic.findRefundChangeItems(refund);

        List<EditSubmitRefundItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitRefundItems();
        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitRefundItem::getChangeSkuCode).collect(Collectors.toList());
        //当前存在的商品
        Map<String,Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        List<String> existSkuCodes = Lists.newArrayList();
        existChangeItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(),refundItem.getApplyQuantity());
            existSkuCodes.add(refundItem.getSkuCode());
        });
        //根据skuCode判断两者是否存在差集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes,existSkuCodes);
        if (commSkuCodes.size()==editSkuCodes.size()&&commSkuCodes.size()==existSkuCodes.size()){
            //如果不存在差集这比较两个商品之间是否存在金额变化的部分
            for (EditSubmitRefundItem editSubmitRefundItem: editSubmitRefundItems){
                Integer existQuantity = existSkuCodeAndQuantity.get(editSubmitRefundItem.getChangeSkuCode());
                if (!Objects.equals(editSubmitRefundItem.getChangeQuantity(),existQuantity)){
                    isChanged = Boolean.TRUE;
                    return isChanged;
                }
            }
        }else{
            //如果存在差集说明有商品变化
            isChanged = Boolean.TRUE;
            return isChanged;
        }
        return isChanged;
    }

    /**
     * 编辑是更新发货单中已经申请退货的数量
     * @param shipmentItems 发货单中的商品集合
     * @param editSubmitRefundInfo 编辑时前端传过来的参数
     * @param refundItems 之前已经选择的售后商品集合
     */
    private void updateShipmentItemRefundQuantityForEdit(List<ShipmentItem> shipmentItems,EditSubmitRefundInfo editSubmitRefundInfo,List<RefundItem> refundItems){
        //前台传输进来的售后商品集合
        List<EditSubmitRefundItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitRefundItems();
        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitRefundItem::getRefundSkuCode).collect(Collectors.toList());
        Map<String,Integer> editSkuCodeAndQuantity = Maps.newHashMap();
        editSubmitRefundItems.forEach(editSubmitRefundItem -> {
            editSkuCodeAndQuantity.put(editSubmitRefundItem.getRefundSkuCode(),editSubmitRefundItem.getRefundQuantity());
        });
        //当前存在的商品
        Map<String,Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        Map<String,RefundItem> skuCodesAndRefundItems = Maps.newHashMap();
        List<String> existSkuCodes = Lists.newArrayList();
        refundItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(),refundItem.getApplyQuantity());
            skuCodesAndRefundItems.put(refundItem.getSkuCode(),refundItem);
            existSkuCodes.add(refundItem.getSkuCode());
        });
        //获取当前传入的skuCode和之前保存的skuCode之间是否存在变化,求交集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes,existSkuCodes);
        if (commSkuCodes.size()==editSkuCodes.size()&&commSkuCodes.size()==existSkuCodes.size()){
            //说明商品没有变化只是改变了数量
            for (EditSubmitRefundItem editSubmitRefundItem: editSubmitRefundItems){
                Integer quantity = editSubmitRefundItem.getRefundQuantity() - existSkuCodeAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()); //只改变了数量
                updateShipmentItemRefundQuantity(editSubmitRefundItem.getRefundSkuCode(),quantity,shipmentItems);
            }
        }else{
            //说明有商品变化
            List<String> editSkuCodeRemain = this.getDifferenceList(editSkuCodes,commSkuCodes); //获取传入的skuCode集合与当前的skuCode集合中不同的部分
            List<String> existSkuCodeRemain = this.getDifferenceList(existSkuCodes,commSkuCodes); //获取当前的skuCode集合与传入的skuCode集合中不同的部分
            for (String skuCode:editSkuCodeRemain){
                updateShipmentItemRefundQuantity(skuCode,editSkuCodeAndQuantity.get(skuCode),shipmentItems);
            }
            for (String skuCode:existSkuCodeRemain){
                updateShipmentItemRefundQuantity(skuCode,-(existSkuCodeAndQuantity.get(skuCode)==null?0:existSkuCodeAndQuantity.get(skuCode)),shipmentItems);
            }
        }
    }


    private void completeWareHoseAndExpressInfo(Integer refundType,RefundExtra refundExtra,EditSubmitRefundInfo submitRefundInfo){

        //非仅退款则验证仓库是否有效、物流信息是否有效
        if(!Objects.equals(refundType, MiddleRefundType.AFTER_SALES_REFUND.value())){
            Warehouse warehouse = findWarehouseById(submitRefundInfo.getWarehouseId());
            refundExtra.setWarehouseId(warehouse.getId());
            refundExtra.setWarehouseName(warehouse.getName());
            refundExtra.setShipmentCorpCode(submitRefundInfo.getShipmentCorpCode());
            refundExtra.setShipmentSerialNo(submitRefundInfo.getShipmentSerialNo());
            refundExtra.setShipmentCorpName(submitRefundInfo.getShipmentCorpName());
        }
    }

    /**
     * 完善换货商品信息
     * @param refundItems 当前需要换货的商品
     * @param refundType 售后类型
     * @param submitRefundInfo 前端提交的售后商品
     * @param extraMap
     */
    private void completeChangeItemInfo(List<RefundItem> refundItems,Integer refundType,EditSubmitRefundInfo submitRefundInfo,Map<String,String> extraMap){
        if(Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(),refundType)){
            //换货数量是否有效
            checkChangeQuantity(submitRefundInfo);
            List<RefundItem> changeItems = makeChangeItemInfo(refundItems,submitRefundInfo);
            completeSkuAttributeInfo(changeItems);
            extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(changeItems)));
        }
    }




    private void checkChangeQuantity(EditSubmitRefundInfo submitRefundInfo){
        List<EditSubmitRefundItem> editSubmitRefundItems = submitRefundInfo.getEditSubmitRefundItems();
        for (EditSubmitRefundItem editSubmitRefundItem:editSubmitRefundItems){
            if(!Objects.equals(editSubmitRefundItem.getRefundQuantity(),editSubmitRefundItem.getChangeQuantity())){
                log.error("refund applyQuantity:{} not equal change applyQuantity:{}",editSubmitRefundItem.getRefundQuantity(),editSubmitRefundItem.getChangeQuantity());
                throw new JsonResponseException("refund.applyQuantity.not.equal.change.applyQuantity");
            }
        }
    }

    /**
     *
     * @param refundItems
     * @param submitRefundInfo
     * @return
     */
    private List<RefundItem> makeChangeItemInfo(List<RefundItem> refundItems,EditSubmitRefundInfo submitRefundInfo){
        List<EditSubmitRefundItem> editSubmitRefundItems = submitRefundInfo.getEditSubmitRefundItems();
        List<RefundItem> changeRefundItems = Lists.newArrayList();
        Map<String,RefundItem> skuCodeAndRefundItemsMap = Maps.newHashMap();
        refundItems.forEach(refundItem -> {
            skuCodeAndRefundItemsMap.put(refundItem.getSkuCode(),refundItem);
        });
        editSubmitRefundItems.forEach(editSubmitRefundItem -> {
            RefundItem changeRefundItem = new RefundItem();
            RefundItem refundItem = skuCodeAndRefundItemsMap.get(editSubmitRefundItem.getRefundSkuCode());
            BeanMapper.copy(refundItem,changeRefundItem);
            changeRefundItem.setApplyQuantity(editSubmitRefundItem.getChangeQuantity());
            changeRefundItem.setAlreadyHandleNumber(0);
            changeRefundItem.setSkuCode(editSubmitRefundItem.getChangeSkuCode());
            changeRefundItem.setFee(editSubmitRefundItem.getFee());
            changeRefundItem.setRefundSkuCode(refundItem.getSkuCode());
            changeRefundItems.add(changeRefundItem);
        });
        return changeRefundItems;

    }

    private Warehouse findWarehouseById(Long warehouseId){

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

    /**
     * 判断退货数量是否有效
     * @param submitRefundInfo 从前端提交过来的请求参数
     * @param shipmentItems 对应发货单的发货商品的集合
     * @return
     */
    private List<RefundItem> checkRefundQuantity(EditSubmitRefundInfo submitRefundInfo,List<ShipmentItem> shipmentItems){
        //退货商品的列表集合
        List<EditSubmitRefundItem> editSubmitRefundItems = submitRefundInfo.getEditSubmitRefundItems();
        //发货单的sku-quantity集合
        Map<String,Integer> skuCodesAndQuantity = Maps.newHashMap();
        List<String> skuCodes = Lists.newArrayList();
        Map<String,ShipmentItem> skuCodesAndShipmentItems = Maps.newHashMap();
        shipmentItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(),shipmentItem.getQuantity());
            skuCodesAndShipmentItems.put(shipmentItem.getSkuCode(),shipmentItem);
            skuCodes.add(shipmentItem.getSkuCode());
        });
        List<RefundItem> refundItems = Lists.newArrayList();
        int count = 0;//判断请求的skuCode在不在申请的发货单中，count>0代表存在skuCode不在发货单中
        List<String> invalidSkuCodes = Lists.newArrayList();
        for (EditSubmitRefundItem editSubmitRefundItem:editSubmitRefundItems){
            if (skuCodes.contains(editSubmitRefundItem.getRefundSkuCode())){
                //判断金额是否小于0
                if (editSubmitRefundItem.getRefundQuantity()<0){
                    log.error("refund applyQuantity:{} invalid",editSubmitRefundItem.getRefundQuantity());
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                //判断申请售后的商品数量是否大于发货单中商品的数量
                if (editSubmitRefundItem.getRefundQuantity()>skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode())){
                    log.error("refund applyQuantity:{} gt available applyQuantity:{}",editSubmitRefundItem.getRefundQuantity(),skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()));
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                RefundItem refundItem = new RefundItem();
                ShipmentItem shipmentItem = skuCodesAndShipmentItems.get(editSubmitRefundItem.getRefundSkuCode());
                BeanMapper.copy(shipmentItem,refundItem);
                refundItem.setApplyQuantity(editSubmitRefundItem.getRefundQuantity());
                refundItems.add(refundItem);
            }else{
                count++;
            }
        }
        if (count==0){
            return refundItems;
        }else{
            log.error("refund sku codes:{} invalid",invalidSkuCodes);
            throw new JsonResponseException("check.refund.applyQuantity.fail");
        }
    }


    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode,Integer refundQuantity,List<ShipmentItem> shipmentItems){
        for (ShipmentItem shipmentItem: shipmentItems){
            if(Objects.equals(skuCode,shipmentItem.getSkuCode())){
                shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity()==null?0:shipmentItem.getRefundQuantity())+refundQuantity);
            }
        }
    }


    private void completeSkuAttributeInfo(List<RefundItem> refundItems){

        List<String> skuCodes = Lists.transform(refundItems, new Function<RefundItem, String>() {
            @Nullable
            @Override
            public String apply(@Nullable RefundItem refundItem) {
                return refundItem.getSkuCode();
            }
        });

        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(skuCodes);
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by sku skuCodes:{} fail,error:{}",skuCodes,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }

        Map<String,SkuTemplate> skuCodeAndTemplateMap =  skuTemplateRes.getResult().stream().filter(Objects::nonNull).filter(it->!Objects.equals(it.getStatus(),-3))
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));
        if (skuCodeAndTemplateMap.size()==0){
            throw new JsonResponseException("sku.may.be.deleted");
        }
        refundItems.forEach(it -> {
            SkuTemplate skuTemplate = skuCodeAndTemplateMap.get(it.getSkuCode());
            it.setAttrs(skuTemplate.getAttrs());
        });
    }

    /**
     * 售后单取消,删除时回滚发货单中退货数量
     * @param refund 售后单
     */
    public  void rollbackRefundQuantities(Refund refund){
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        long shipmentId = refundExtra.getShipmentId();
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String,RefundItem> skuCodeAndRefundItemMap = refundItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(RefundItem::getSkuCode, it -> it));

        shipmentItems.forEach(it -> {
            RefundItem refundItem = skuCodeAndRefundItemMap.get(it.getSkuCode());
            if (refundItem!=null)
            {
                it.setRefundQuantity(it.getRefundQuantity()-refundItem.getApplyQuantity());
            }

        });
        Map<String,String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO,JsonMapper.nonEmptyMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(),shipmentExtraMap);
    }


    /**
     * 售后单客服备注
     * @param refundId 售后单主键
     * @param customerServiceNote 客服备注
     */
    public void addCustomerServiceNote(long refundId,String  customerServiceNote){

        Response<Boolean> response = refundWriteService.updateSellerNote(refundId,customerServiceNote);
        if (!response.isSuccess()){
            log.error("refund add customerServiceNote failed,refundId is({}),caused by{}",refundId,response.getError());
            throw new JsonResponseException("add customer service note fail");
        }
    }


    /**
     * 创建丢件补发类型类型的逆向单
     * @param submitRefundInfo 参数实体
     * @return
     */
    public Long createRefundForLost(SubmitRefundInfo submitRefundInfo){
        //获取订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //获取订单下的所有发货单
        List<Shipment> originShipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
        List<Shipment> shipments = originShipments.stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        //获取订单下对应发货单的所有发货商品列表
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItemsForList(shipments);
        //申请数量是否有效
        List<RefundItem> refundItems = checkChangeItemsForLost(submitRefundInfo,shipmentItems);
        completeSkuAttributeInfo(refundItems);
        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setRefundAt(new Date());
        if(Objects.equals(submitRefundInfo.getOperationType(),1)){
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }else {
            refund.setStatus(MiddleRefundStatus.LOST_WAIT_CREATE_SHIPMENT.getValue());
        }
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setFee(shopOrder.getFee());
        refund.setRefundType(submitRefundInfo.getRefundType());
        Map<String,String> extraMap = Maps.newHashMap();
        RefundExtra refundExtra = new RefundExtra();
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipments.get(0).getReceiverInfos(),ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_LOST_ITEM_INFO,mapper.toJson(refundItems));
        //完善换货信息
        //完善换货发货地址信息
        if (Objects.equals(MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value(),submitRefundInfo.getRefundType())){
            if (Objects.nonNull(submitRefundInfo.getMiddleChangeReceiveInfo())){
                extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO,mapper.toJson(submitRefundInfo.getMiddleChangeReceiveInfo()));
            }
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG,"0");
        refund.setExtra(extraMap);
        //打标
        Map<String,String> tagMap = Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.MANUAL.value()));
        refund.setTags(tagMap);
        //创建售后单
        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(submitRefundInfo.getOrderId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }
        return rRefundRes.getResult();
    }
    /**
     * 丢件补发参数组装
     * @param submitRefundInfo
     * @param shipmentItems 该订单下的所有的商品集合
     * @return
     */
    private List<RefundItem> checkChangeItemsForLost(EditSubmitRefundInfo submitRefundInfo,List<ShipmentItem> shipmentItems){
        //可能存在只有部分商品被弄丢了，这个时候需要传输商品条码
        //传输过来的需要丢件补发的商品集合
        List<ShipmentItem> lostItems = submitRefundInfo.getLostItems();
        //获取需要丢件补发的skuCode的集合
        List<String> changeSkuCodes = lostItems.stream().filter(Objects::nonNull).map(ShipmentItem::getSkuCode).collect(Collectors.toList());
        List<RefundItem>   refundItems = Lists.newArrayList();
        Map<String,Integer> skuCodesAndQuantity = Maps.newHashMap();
        lostItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(),shipmentItem.getQuantity());
        });
        shipmentItems.forEach(shipmentItem -> {
            //获取所有需要补发的RefundItem
            if (changeSkuCodes.contains(shipmentItem.getSkuCode())){
                RefundItem refundItem = new RefundItem();
                BeanMapper.copy(shipmentItem,refundItem);
                refundItem.setApplyQuantity(skuCodesAndQuantity.get(shipmentItem.getSkuCode()));
                refundItem.setSkuCode(shipmentItem.getSkuCode());
                refundItems.add(refundItem);
            }
        });
        return refundItems;
    }

    /**
     * 丢件补发类型的售后单完善
     * @param refund
     * @param submitRefundInfo
     */
    public void completeHandleForLostType(Refund refund, EditSubmitRefundInfo submitRefundInfo){
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        //获取订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        //获取订单下的所有发货单
        List<Shipment> originShipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
        List<Shipment> shipments = originShipments.stream().
                filter(Objects::nonNull).filter(shipment -> !Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                .collect(Collectors.toList());
        //获取订单下对应发货单的所有发货商品列表
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItemsForList(shipments);
        //获取最新的丢件补发的商品
        List<RefundItem> cureentLostRefundItems = checkChangeItemsForLost(submitRefundInfo,shipmentItems);
        completeSkuAttributeInfo(cureentLostRefundItems);

        Map<String,String> extraMap = refund.getExtra();
        //添加处理完成时间
        refundExtra.setHandleDoneAt(new Date());
        Refund updateRefund = new Refund();
        updateRefund.setId(refund.getId());
        updateRefund.setBuyerNote(submitRefundInfo.getBuyerNote());
        updateRefund.setFee(shopOrder.getFee());

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_LOST_ITEM_INFO,mapper.toJson(cureentLostRefundItems));
        //提交动作
        if(Objects.equals(submitRefundInfo.getOperationType(),2)){
            //更新售后单状态
            Response<Boolean> updateStatusRes = updateStatus(refund,MiddleOrderEvent.LOST_HANDLE.toOrderOperation());
            if(!updateStatusRes.isSuccess()){
                log.error("update refund(id:{}) status to:{} fail,error:{}",refund.getId(),updateStatusRes.getError());
                throw new JsonResponseException(updateStatusRes.getError());
            }
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG,"0");
        updateRefund.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(updateRefund);
        if(!updateRes.isSuccess()){
            log.error("update refund:{} fail,error:{}",updateRefund,updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    /**
     * 丢件补发类型的售后单的删除
     * @param refund 售后单
     */
    public void deleteRefundForLost(Refund refund){
        //判断类型
        RefundSource refundSource = refundReadLogic.findRefundSource(refund);
        if(Objects.equals(refundSource.value(),RefundSource.THIRD.value())){
            log.error("refund(id:{}) is third party refund  so cant not delete",refund.getId());
            throw new JsonResponseException("third.party.refund.can.not.delete");
        }
        //更新状态
        Response<Boolean> updateRes = this.updateStatus(refund,MiddleOrderEvent.DELETE.toOrderOperation());
        if(!updateRes.isSuccess()){
            log.error("delete refund(id:{}) fail,error:{}",refund.getId(),updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    /**
     * 获取两个集合的交集
     * @param list1
     * @param list2
     * @return
     */
    public List<String> getDntersectionList(List<String> list1, List<String> list2){
        Set<String> sets = Sets.newHashSet(list1);
        Set<String> sets2 = Sets.newHashSet(list2);
        SetView<String> intersection = Sets.intersection(sets, sets2);
        return new ArrayList<>(intersection);
    }

    /**
     * 获取两个集合的差集
     * @param list1
     * @param list2
     * @return
     */
    public List<String> getDifferenceList(List<String> list1, List<String> list2){
        Set<String> sets = Sets.newHashSet(list1);
        Set<String> sets2 = Sets.newHashSet(list2);
        SetView<String> intersection = Sets.difference(sets, sets2);
        return new ArrayList<>(intersection);
    }

}
