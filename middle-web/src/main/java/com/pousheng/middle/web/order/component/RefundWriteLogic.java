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
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                refundItem.setAlreadyHandleNumber(refundItem.getAlreadyHandleNumber() + skuCodeAndQuantity.get(refundItem.getSkuCode()));
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

        return Response.ok();

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
        RefundItem refundItem = checkRefundQuantity(submitRefundInfo,shipmentItems);
        completeSkuAttributeInfo(Lists.newArrayList(refundItem));
        updateShipmentItemRefundQuantity(submitRefundInfo.getRefundSkuCode(),submitRefundInfo.getRefundQuantity(),shipmentItems);

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
        extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(refundItem)));
        //完善换货信息
        completeChangeItemInfo(refundItem,submitRefundInfo.getRefundType(),submitRefundInfo,extraMap);
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


    public void completeHandle(Refund refund, EditSubmitRefundInfo submitRefundInfo){

        List<RefundItem> existRefundItems = refundReadLogic.findRefundItems(refund);
        RefundItem existRefundItem = existRefundItems.get(0);//只会存在一条 退货商品
        RefundItem currentRefundItem = existRefundItem;//当前编辑情况下的退货商品
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
        Boolean isRefundItemChanged = refundItemIsChanged(submitRefundInfo,existRefundItem);
        if(isRefundItemChanged){
            //申请数量是否有效
            currentRefundItem = checkRefundQuantity(submitRefundInfo,shipmentItems);
            //更新发货商品中的已退货数量
            updateShipmentItemRefundQuantityForEdit(shipmentItems,submitRefundInfo,existRefundItem);
            completeSkuAttributeInfo(Lists.newArrayList(currentRefundItem));
            extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(currentRefundItem)));
        }

        //判断换货货商品及数量是否有变化
        Boolean isChangeItemChanged = changeItemIsChanged(refund,submitRefundInfo);
        if(isChangeItemChanged){
            //完善换货信息
            completeChangeItemInfo(currentRefundItem,refund.getRefundType(),submitRefundInfo,extraMap);
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
                currentRefundItem.setFee(submitRefundInfo.getFee());
                extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(Lists.newArrayList(currentRefundItem)));

            }

            //如果只是改了退款金额,则更新换货商品中的退款金额
            if(!isChangeItemChanged&&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())){
                RefundItem existChangeItem = refundReadLogic.findRefundChangeItems(refund).get(0);
                existChangeItem.setFee(submitRefundInfo.getFee());
                extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(existChangeItem)));
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

    //退货商品是否改变
    private Boolean refundItemIsChanged(EditSubmitRefundInfo editSubmitRefundInfo,RefundItem existRefundItem){
        Boolean isChanged = Boolean.FALSE;

        if(!Objects.equals(editSubmitRefundInfo.getRefundQuantity(),existRefundItem.getApplyQuantity())){
            isChanged = Boolean.TRUE;
        }

        if(!Objects.equals(editSubmitRefundInfo.getRefundSkuCode(),existRefundItem.getSkuCode())){
            isChanged = Boolean.TRUE;
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
        RefundItem existChangeItem = existChangeItems.get(0);//只会存在一条 换货商品

        if(!Objects.equals(editSubmitRefundInfo.getRefundQuantity(),existChangeItem.getApplyQuantity())){
            isChanged = Boolean.TRUE;
        }

        if(!Objects.equals(editSubmitRefundInfo.getRefundSkuCode(),existChangeItem.getSkuCode())){
            isChanged = Boolean.TRUE;
        }

        return isChanged;
    }

    private void updateShipmentItemRefundQuantityForEdit(List<ShipmentItem> shipmentItems,EditSubmitRefundInfo editSubmitRefundInfo,RefundItem refundItem){

        //编辑前后商品没改变只改变了数量
        if(Objects.equals(editSubmitRefundInfo.getRefundSkuCode(),refundItem.getSkuCode())){
            //计算出变化量
            Integer quantity = editSubmitRefundInfo.getRefundQuantity() - refundItem.getApplyQuantity();
            updateShipmentItemRefundQuantity(editSubmitRefundInfo.getRefundSkuCode(),quantity,shipmentItems);
        }else {
            //改变了商品
            updateShipmentItemRefundQuantity(editSubmitRefundInfo.getRefundSkuCode(),editSubmitRefundInfo.getRefundQuantity(),shipmentItems);
            updateShipmentItemRefundQuantity(refundItem.getSkuCode(),-(refundItem.getApplyQuantity()==null?0:refundItem.getApplyQuantity()),shipmentItems);

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


    private void completeChangeItemInfo(RefundItem refundItem,Integer refundType,EditSubmitRefundInfo submitRefundInfo,Map<String,String> extraMap){
        if(Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(),refundType)){
            //换货数量是否有效
            checkChangeQuantity(submitRefundInfo);
            RefundItem changeItem = makeChangeItemInfo(refundItem,submitRefundInfo);
            completeSkuAttributeInfo(Lists.newArrayList(changeItem));
            extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO,mapper.toJson(Lists.newArrayList(changeItem)));
        }
    }




    private void checkChangeQuantity(EditSubmitRefundInfo submitRefundInfo){
        if(!Objects.equals(submitRefundInfo.getRefundQuantity(),submitRefundInfo.getChangeQuantity())){
            log.error("refund applyQuantity:{} not equal change applyQuantity:{}",submitRefundInfo.getRefundQuantity(),submitRefundInfo.getChangeQuantity());
            throw new JsonResponseException("refund.applyQuantity.not.equal.change.applyQuantity");
        }
    }

    private RefundItem makeChangeItemInfo(RefundItem refundItem,EditSubmitRefundInfo submitRefundInfo){

        //todo 封装换货商品信息
        RefundItem changeRefundItem = new RefundItem();
        BeanMapper.copy(refundItem,changeRefundItem);
        changeRefundItem.setApplyQuantity(submitRefundInfo.getChangeQuantity());
        changeRefundItem.setAlreadyHandleNumber(0);
        changeRefundItem.setSkuCode(submitRefundInfo.getChangeSkuCode());
        changeRefundItem.setItemId(refundItem.getItemId());
        changeRefundItem.setAttrs(refundItem.getAttrs());
        return changeRefundItem;

    }

    private Warehouse findWarehouseById(Long warehouseId){

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }


    private RefundItem checkRefundQuantity(EditSubmitRefundInfo submitRefundInfo,List<ShipmentItem> shipmentItems){
        for (ShipmentItem shipmentItem : shipmentItems){
            if(Objects.equals(submitRefundInfo.getRefundSkuCode(),shipmentItem.getSkuCode())){
                /*Integer availableQuantity = shipmentItem.getQuantity()-shipmentItem.getRefundQuantity();*/
                if(submitRefundInfo.getRefundQuantity()<=0){
                    log.error("refund applyQuantity:{} invalid",submitRefundInfo.getRefundQuantity());
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                if(submitRefundInfo.getRefundQuantity()>shipmentItem.getQuantity()){
                    log.error("refund applyQuantity:{} gt available applyQuantity:{}",submitRefundInfo.getRefundQuantity(),shipmentItem.getQuantity());
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                RefundItem refundItem = new RefundItem();
                BeanMapper.copy(shipmentItem,refundItem);
                refundItem.setApplyQuantity(submitRefundInfo.getRefundQuantity());
                return refundItem;
            }
        }
        log.error("refund sku code:{} invalid",submitRefundInfo.getRefundSkuCode());
        throw new JsonResponseException("check.refund.applyQuantity.fail");

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
}
