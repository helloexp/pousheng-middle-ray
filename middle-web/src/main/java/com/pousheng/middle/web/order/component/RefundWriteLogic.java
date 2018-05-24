package com.pousheng.middle.web.order.component;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.model.RefundAmount;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.RefundAmountWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
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
import org.springframework.util.StringUtils;

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
    private SyncErpReturnLogic syncErpReturnLogic;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @Autowired
    private RefundAmountWriteService refundAmountWriteService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SyncRefundLogic syncRefundLogic;
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    /**
     * 更新换货商品处理数量
     * 判断是否商品已全部处理，如果是则更新状态为 WAIT_SHIP:待发货
     *
     * @param skuCodeAndQuantity 商品编码及数量
     */
    public boolean updateSkuHandleNumber(Long refundId, Map<String, Integer> skuCodeAndQuantity) {

        boolean result = true;

        Refund refund = refundReadLogic.findRefundById(refundId);
        //换货商品
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //是否全部处理
        Boolean isAllHandle = Boolean.TRUE;
        //更新发货数量
        for (RefundItem refundItem : refundChangeItems) {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundItem.setAlreadyHandleNumber((refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber()) + skuCodeAndQuantity.get(refundItem.getSkuCode()));
            }

            //如果存在未处理完成的
            if (!Objects.equals(refundItem.getApplyQuantity(), (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber()))) {
                isAllHandle = Boolean.FALSE;
            } else {
                //换货商品已经全部处理完,此时处于待发货状态,此时填入换货发货单创建时间
                refundExtra.setChangeShipmentAt(new Date());
            }
        }

        Refund update = new Refund();
        update.setId(refundId);
        Map<String, String> extrMap = refund.getExtra();
        extrMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundChangeItems));
        extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        update.setExtra(extrMap);

        Response<Boolean> updateRes = refundWriteService.update(update);
        if (!updateRes.isSuccess()) {
            result = false;
            log.error("update refund(id:{}) fail,error:{}", refund, updateRes.getError());
        }

        if (isAllHandle) {
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.CREATE_SHIPMENT.toOrderOperation());
            Response<Boolean> updateStatusRes = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refundId,refund.getStatus(), targetStatus);
            if (!updateStatusRes.isSuccess()) {
                result = false;
                log.error("update refund(id:{}) status to:{} fail,error:{}", refund, targetStatus, updateRes.getError());
            }
        }
        return result;
    }

    /**
     * 更新丢件补发类型售后单已经处理数量
     * 判断是否商品已全部处理，如果是则更新状态为 WAIT_SHIP:待发货
     *
     * @param skuCodeAndQuantity 商品编码及数量
     */
    public boolean updateSkuHandleNumberForLost(Long refundId, Map<String, Integer> skuCodeAndQuantity) {

        boolean result = true;
        Refund refund = refundReadLogic.findRefundById(refundId);
        //丢件补发商品
        List<RefundItem> refundLostItems = refundReadLogic.findRefundLostItems(refund);

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //是否全部处理
        Boolean isAllHandle = Boolean.TRUE;
        //更新发货数量
        for (RefundItem refundItem : refundLostItems) {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundItem.setAlreadyHandleNumber((refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber()) + skuCodeAndQuantity.get(refundItem.getSkuCode()));
            }

            //如果存在未处理完成的
            if (!Objects.equals(refundItem.getApplyQuantity(), (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber()))) {
                isAllHandle = Boolean.FALSE;
            } else {
                //换货商品已经全部处理完,此时处于待发货状态,此时填入换货发货单创建时间
                refundExtra.setLostShipmentAt(new Date());
            }
        }

        Refund update = new Refund();
        update.setId(refundId);
        Map<String, String> extrMap = refund.getExtra();
        extrMap.put(TradeConstants.REFUND_LOST_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(refundLostItems));
        extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        update.setExtra(extrMap);

        Response<Boolean> updateRes = refundWriteService.update(update);
        if (!updateRes.isSuccess()) {
            result = false;
            log.error("update refund(id:{}) fail,error:{}", refund, updateRes.getError());
        }

        if (isAllHandle) {
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.LOST_CREATE_SHIP.toOrderOperation());
            Response<Boolean> updateStatusRes = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refundId,refund.getStatus(), targetStatus);
            if (!updateStatusRes.isSuccess()) {
                result = false;
                log.error("update refund(id:{}) status to:{} fail,error:{}", refund, targetStatus, updateRes.getError());
            }
        }
        return result;

    }


    public Response<Boolean> updateStatus(Refund refund, OrderOperation orderOperation) {

        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(refund.getStatus(), orderOperation)) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), orderOperation.getText());
            return Response.fail("refund.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(refund.getStatus(), orderOperation);
        Response<Boolean> updateRes = refundWriteService.updateStatus(refund.getId(), targetStatus);
        if (!updateRes.isSuccess()) {
            log.error("update refund(id:{}) status to:{} fail,error:{}", refund.getId(), updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok(Boolean.TRUE);

    }

    /**
     * 以乐观锁的方式更新售后单状态
     * @param refund 售后单
     * @param orderOperation 状态机节点
     * @return
     */
    public Response<Boolean> updateStatusLocking(Refund refund, OrderOperation orderOperation) {

        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(refund.getStatus(), orderOperation)) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), orderOperation.getText());
            return Response.fail("refund.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(refund.getStatus(), orderOperation);
        Response<Boolean> updateRes = refundWriteService.updateStatusByRefundIdAndCurrentStatus(refund.getId(),refund.getStatus(), targetStatus);
        if (!updateRes.isSuccess()) {
            log.error("update refund(id:{}) status to:{} fail,error:{}", refund.getId(), updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok(Boolean.TRUE);

    }


    public Response<Boolean> update(Refund refund) {

        Response<Boolean> updateRes = refundWriteService.update(refund);
        if (!updateRes.isSuccess()) {
            log.error("update refund({}) status to:{} fail,error:{}", refund, updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok();

    }

    //删除逆向订单 限手动创建的
    public void deleteRefund(Refund refund) {
        //判断类型 -----去除第三方售后单不能删除的逻辑
        /*RefundSource refundSource = refundReadLogic.findRefundSource(refund);
        if (Objects.equals(refundSource.value(), RefundSource.THIRD.value())) {
            log.error("refund(id:{}) is third party refund  so cant not delete", refund.getId());
            throw new JsonResponseException("third.party.refund.can.not.delete");
        }*/

        //退货信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Map<String, RefundItem> skuCodeAndRefundItemMap = refundItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(RefundItem::getSkuCode, it -> it));


        //退货单所属发货单信息
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);


        //更新状态
        Response<Boolean> updateRes = this.updateStatusLocking(refund, MiddleOrderEvent.DELETE.toOrderOperation());
        if (!updateRes.isSuccess()) {
            log.error("delete refund(id:{}) fail,error:{}", refund.getId(), updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
        //回滚发货单的已退货数量
        shipmentItems.forEach(it -> {
            RefundItem refundItem = skuCodeAndRefundItemMap.get(it.getSkuCode());
            if (refundItem != null) {
                it.setRefundQuantity(it.getRefundQuantity() - refundItem.getApplyQuantity());
            }

        });


        //更新发货单商品中的已退货数量
        Map<String, String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonDefaultMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
    }


    /**
     * 新增仅退款，退货，售后类型的售后单
     * 支持对于换货售后单继续售后
     *
     * @param submitRefundInfo
     * @return
     */
    public Long createRefund(SubmitRefundInfo submitRefundInfo) {
        //验证提交信息是否有效
        //订单是否有效
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(submitRefundInfo.getOrderCode());
//        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //发货单是否有效
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(submitRefundInfo.getShipmentCode());
        //获取发货单中商品信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //申请数量是否有效
        List<RefundItem> refundItems = checkRefundQuantity(submitRefundInfo, shipmentItems);
        //更新货品信息
        completeSkuAttributeInfo(refundItems);
        //更新金额
        if (!Objects.equals(submitRefundInfo.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
            this.calcRefundItemFees(refundItems, submitRefundInfo.getFee());
        }
        for (EditSubmitRefundItem editSubmitRefundItem : submitRefundInfo.getEditSubmitRefundItems()) {
            //更新发货单中已经售后的商品的数量
            updateShipmentItemRefundQuantity(editSubmitRefundItem.getRefundSkuCode(), editSubmitRefundItem.getRefundQuantity(), shipmentItems);
        }

        //组装售后单参数
        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setRefundAt(new Date());
        if (Objects.equals(submitRefundInfo.getOperationType(), 1)) {
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        } else {
            refund.setStatus(MiddleRefundStatus.WAIT_SYNC_HK.getValue());
        }
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setRefundType(submitRefundInfo.getRefundType());

        Map<String, String> extraMap = Maps.newHashMap();

        RefundExtra refundExtra = new RefundExtra();

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(), ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        refundExtra.setShipmentId(shipment.getShipmentCode());
        //关联单号
        refundExtra.setReleOrderNo(submitRefundInfo.getReleOrderNo());
        //关联单号类型
        refundExtra.setReleOrderType(submitRefundInfo.getReleOrderType());
        refund.setReleOrderCode(submitRefundInfo.getReleOrderNo());
        //完善仓库及物流信息
        completeWareHoseAndExpressInfo(shipment,submitRefundInfo.getRefundType(), refundExtra, submitRefundInfo);

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(refundItems));
        //完善换货信息
        completeChangeItemInfo(refundItems, submitRefundInfo.getRefundType(), submitRefundInfo, extraMap);
        //完善换货发货地址信息
        if (Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(), submitRefundInfo.getRefundType())) {
            if (Objects.nonNull(submitRefundInfo.getMiddleChangeReceiveInfo())) {
                extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO, mapper.toJson(submitRefundInfo.getMiddleChangeReceiveInfo()));
            }
        }
        if (Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())||Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
            //换货的金额用商品净价*申请数量
            Long totalRefundAmount = 0L;
            for (RefundItem refundItem :refundItems){
                totalRefundAmount = totalRefundAmount+refundItem.getCleanPrice()*refundItem.getApplyQuantity();
            }
            refund.setFee(totalRefundAmount);
        }else{
            refund.setFee(submitRefundInfo.getFee());
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
        refund.setExtra(extraMap);
        //打标
        Map<String, String> tagMap = Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.MANUAL.value()));
        refund.setTags(tagMap);


        //创建售后单
        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(shopOrder.getId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }

        //更新发货单商品中的已退货数量
        Map<String, String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
        //如果是手工创建的售后单是点击的提交，直接同步恒康
        if (Objects.equals(submitRefundInfo.getOperationType(), 2)) {
            Refund newRefund = refundReadLogic.findRefundById(refund.getId());
            Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(newRefund);
            if (!syncRes.isSuccess()) {
                log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
            }
        }
        return rRefundRes.getResult();
    }

    /**
     * 完善售后单
     * 完善售后单（仅退款，退货退款，换货）
     *
     * @param refund           售后单
     * @param submitRefundInfo 前端传入的参数
     */
    public void completeHandle(Refund refund, EditSubmitRefundInfo submitRefundInfo) {
        //获取当前数据库中已经存在的售后商品信息
        List<RefundItem> existRefundItems = refundReadLogic.findRefundItems(refund);
        List<RefundItem> currentRefundItems = existRefundItems;//当前编辑情况下的退货商品
        //获取extra信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //发货单信息
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId() == null ? submitRefundInfo.getShipmentCode() : refundExtra.getShipmentId());
        //获取售后单相应的发货单的信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String, String> extraMap = refund.getExtra();

        //完善仓库及物流信息
        completeWareHoseAndExpressInfo(shipment,refund.getRefundType(), refundExtra, submitRefundInfo);
        //添加处理完成时间
        refundExtra.setHandleDoneAt(new Date());
        //todo 是否可以修改售后单id
        if (refundExtra.getShipmentId() == null) {
            refundExtra.setShipmentId(submitRefundInfo.getShipmentCode());
        }
        //更新售后单信息
        Refund updateRefund = new Refund();
        updateRefund.setId(refund.getId());
        //更新买家备注
        updateRefund.setBuyerNote(submitRefundInfo.getBuyerNote());
        //更新退款金额
        updateRefund.setFee(submitRefundInfo.getFee());

        //判断退货商品及数量是否有变化
        Boolean isRefundItemChanged = refundItemIsChanged(submitRefundInfo, existRefundItems);
        if (isRefundItemChanged) {
            //申请数量是否有效
            currentRefundItems = checkRefundQuantity(submitRefundInfo, shipmentItems);
            //更新发货商品中的已退货数量
            updateShipmentItemRefundQuantityForEdit(shipmentItems, submitRefundInfo, existRefundItems);
            //完善货品信息
            completeSkuAttributeInfo(currentRefundItems);
        }
        //更新金额
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
            this.calcRefundItemFees(currentRefundItems, submitRefundInfo.getFee());
        }
        extraMap.put(TradeConstants.REFUND_ITEM_INFO, mapper.toJson(currentRefundItems));

        //判断换货货商品及数量是否有变化
        Boolean isChangeItemChanged = changeItemIsChanged(refund, submitRefundInfo);
        if (isChangeItemChanged) {
            //完善换货信息
            completeChangeItemInfo(currentRefundItems, refund.getRefundType(), submitRefundInfo, extraMap);
        }
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));

        //提交动作
        if (Objects.equals(submitRefundInfo.getOperationType(), 2)) {
            //更新售后单状态
            Response<Boolean> updateStatusRes = updateStatusLocking(refund, MiddleOrderEvent.HANDLE.toOrderOperation());
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status to:{} fail,error:{}", refund.getId(), updateStatusRes.getError());
                throw new JsonResponseException(updateStatusRes.getError());
            }
        }

        if (Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())||Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
            //换货的金额用商品净价*申请数量
            Long totalRefundAmount = 0L;
        //更新退换货信息
        //退款总金额变化
            for (RefundItem refundItem :currentRefundItems){
                totalRefundAmount = totalRefundAmount+refundItem.getCleanFee()*refundItem.getApplyQuantity();
            }
            updateRefund.setFee(totalRefundAmount);
        }else{
            updateRefund.setFee(submitRefundInfo.getFee());
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");

        updateRefund.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(updateRefund);
        if (!updateRes.isSuccess()) {
            log.error("update refund:{} fail,error:{}", updateRefund, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

        //如果退货商品信息变了则更新对应发货商品的退货数量
        if (isRefundItemChanged) {
            //更新发货单商品中的已退货数量
            Map<String, String> shipmentExtraMap = shipment.getExtra();
            shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, mapper.toJson(shipmentItems));
            shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
        }


    }

    //创建售后单
    public Long createYunJURefund(SubmitRefundInfo submitRefundInfo){
        // 退款金额
        Long refundFee = 0L;
        //验证提交信息是否有效
        //订单是否有效
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        //发货单是否有效
        Shipment shipment = shipmentReadLogic.findShipmentById(submitRefundInfo.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        //申请数量是否有效
        List<RefundItem> refundItems = checkYunJuRefundQuantity(submitRefundInfo,shipmentItems);
        completeSkuAttributeInfo(refundItems);
//        //更新金额
//        if (!Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value()))
//        {
//            this.calcRefundItemFees(refundItems,submitRefundInfo.getFee());
//        }
        for (EditSubmitRefundItem editSubmitRefundItem:submitRefundInfo.getEditSubmitRefundItems()){
            updateShipmentItemRefundQuantity(editSubmitRefundItem.getRefundSkuCode(),editSubmitRefundItem.getRefundQuantity(),shipmentItems);
            refundFee += editSubmitRefundItem.getFee();
        }
        Refund refund = new Refund();
        // 售后单关联单号 订单的订单编号
        refund.setReleOrderCode(shopOrder.getOrderCode());
        refund.setOutId(MiddleChannel.YJ.getValue()+"_"+submitRefundInfo.getOutAfterSaleOrderId());//outId是 yunju_售后单号
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setRefundAt(new Date());

        // 云聚单子 默认传1
        if(Objects.equals(submitRefundInfo.getOperationType(),1)){
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        }else {
            refund.setStatus(MiddleRefundStatus.WAIT_SYNC_HK.getValue());
        }

        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setRefundType(submitRefundInfo.getRefundType());

        Map<String,String> extraMap = Maps.newHashMap();

        RefundExtra refundExtra = new RefundExtra();

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(),ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        refundExtra.setShipmentId(shipment.getShipmentCode());
        //完善退货仓库及物流信息
        completeYunJUWareHoseAndExpressInfo(shopOrder,submitRefundInfo.getRefundType(),refundExtra,submitRefundInfo);

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

        // 云聚接口传了退款金额 就使用接口参数，没有传就使用子订单中的退款金额累加
        Long refundFeeTotal = MoreObjects.firstNonNull(submitRefundInfo.getFee(), refundFee);
        log.info("refund fee is : {}", refundFeeTotal);
        refund.setFee(refundFeeTotal);

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
            Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(newRefund);
            if (!syncRes.isSuccess()) {
                log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
            }
        }
        return rRefundRes.getResult();
    }

    /**
     * 判断退换货商品是否已经改变
     *
     * @param editSubmitRefundInfo
     * @param existRefundItems     当前存在的sku
     * @return
     */
    private Boolean refundItemIsChanged(EditSubmitRefundInfo editSubmitRefundInfo, List<RefundItem> existRefundItems) {
        Boolean isChanged = Boolean.FALSE;
        //传输进来的售后商品
        List<EditSubmitRefundItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitRefundItems();
        //获取新传入的skuCode的集合
        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitRefundItem::getRefundSkuCode).collect(Collectors.toList());
        //之前编辑的售后商品
        Map<String, Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        //获取已经存在的skuCode集合
        List<String> existSkuCodes = existRefundItems.stream().filter(Objects::nonNull).map(RefundItem::getSkuCode).collect(Collectors.toList());
        existRefundItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(), refundItem.getApplyQuantity());
        });
        //获取交集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes, existSkuCodes);
        //如果交集等于他们各自的数量，则表明没有skuCode变化
        if (commSkuCodes.size() == editSkuCodes.size() && commSkuCodes.size() == existSkuCodes.size()) {
            //如果不存在差集这比较两个商品之间是否存在金额变化的部分
            for (EditSubmitRefundItem editSubmitRefundItem : editSubmitRefundItems) {
                Integer existQuantity = existSkuCodeAndQuantity.get(editSubmitRefundItem.getRefundSkuCode());
                if (!Objects.equals(editSubmitRefundItem.getRefundQuantity(), existQuantity)) {
                    isChanged = Boolean.TRUE;
                    return isChanged;
                }
            }
        } else {
            //如果存在差集说明有商品变化
            isChanged = Boolean.TRUE;
            return isChanged;
        }
        return isChanged;
    }


    //换货商品是否改变
    private Boolean changeItemIsChanged(Refund refund, EditSubmitRefundInfo editSubmitRefundInfo) {

        Boolean isChanged = Boolean.FALSE;

        //不为换货售后则直接返回false
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
            return Boolean.FALSE;
        }
        //获取当前已经选择好的售后商品的集合
        List<RefundItem> existChangeItems = refundReadLogic.findRefundChangeItems(refund);

        List<EditSubmitChangeItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitChangeItems();

        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitChangeItem::getChangeSkuCode).collect(Collectors.toList());
        //当前存在的商品
        Map<String, Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        List<String> existSkuCodes = Lists.newArrayList();
        existChangeItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(), refundItem.getApplyQuantity());
            existSkuCodes.add(refundItem.getSkuCode());
        });
        //根据skuCode判断两者是否存在差集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes, existSkuCodes);
        if (commSkuCodes.size() == editSkuCodes.size() && commSkuCodes.size() == existSkuCodes.size()) {
            //如果不存在差集这比较两个商品之间是否存在金额变化的部分
            for (EditSubmitChangeItem editSubmitRefundItem : editSubmitRefundItems) {
                Integer existQuantity = existSkuCodeAndQuantity.get(editSubmitRefundItem.getChangeSkuCode());
                if (!Objects.equals(editSubmitRefundItem.getChangeQuantity(), existQuantity)) {
                    isChanged = Boolean.TRUE;
                    return isChanged;
                }
            }
        } else {
            //如果存在差集说明有商品变化
            isChanged = Boolean.TRUE;
            return isChanged;
        }
        return isChanged;
    }

    /**
     * 编辑是更新发货单中已经申请退货的数量
     *
     * @param shipmentItems        发货单中的商品集合
     * @param editSubmitRefundInfo 编辑时前端传过来的参数
     * @param refundItems          之前已经选择的售后商品集合
     */
    private void updateShipmentItemRefundQuantityForEdit(List<ShipmentItem> shipmentItems, EditSubmitRefundInfo editSubmitRefundInfo, List<RefundItem> refundItems) {
        //前台传输进来的售后商品集合
        List<EditSubmitRefundItem> editSubmitRefundItems = editSubmitRefundInfo.getEditSubmitRefundItems();
        List<String> editSkuCodes = editSubmitRefundItems.stream().filter(Objects::nonNull).map(EditSubmitRefundItem::getRefundSkuCode).collect(Collectors.toList());
        Map<String, Integer> editSkuCodeAndQuantity = Maps.newHashMap();
        editSubmitRefundItems.forEach(editSubmitRefundItem -> {
            editSkuCodeAndQuantity.put(editSubmitRefundItem.getRefundSkuCode(), editSubmitRefundItem.getRefundQuantity());
        });
        //当前存在的商品
        Map<String, Integer> existSkuCodeAndQuantity = Maps.newHashMap();
        Map<String, RefundItem> skuCodesAndRefundItems = Maps.newHashMap();
        List<String> existSkuCodes = Lists.newArrayList();
        refundItems.forEach(refundItem -> {
            existSkuCodeAndQuantity.put(refundItem.getSkuCode(), refundItem.getApplyQuantity());
            skuCodesAndRefundItems.put(refundItem.getSkuCode(), refundItem);
            existSkuCodes.add(refundItem.getSkuCode());
        });
        //获取当前传入的skuCode和之前保存的skuCode之间是否存在变化,求交集
        List<String> commSkuCodes = this.getDntersectionList(editSkuCodes, existSkuCodes);
        if (commSkuCodes.size() == editSkuCodes.size() && commSkuCodes.size() == existSkuCodes.size()) {
            //说明商品没有变化只是改变了数量
            for (EditSubmitRefundItem editSubmitRefundItem : editSubmitRefundItems) {
                Integer quantity = editSubmitRefundItem.getRefundQuantity() - existSkuCodeAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()); //只改变了数量
                updateShipmentItemRefundQuantity(editSubmitRefundItem.getRefundSkuCode(), quantity, shipmentItems);
            }
        } else {
            //说明有商品变化
            List<String> editSkuCodeRemain = this.getDifferenceList(editSkuCodes, commSkuCodes); //获取传入的skuCode集合与当前的skuCode集合中不同的部分
            List<String> existSkuCodeRemain = this.getDifferenceList(existSkuCodes, commSkuCodes); //获取当前的skuCode集合与传入的skuCode集合中不同的部分
            for (String skuCode : editSkuCodeRemain) {
                updateShipmentItemRefundQuantity(skuCode, editSkuCodeAndQuantity.get(skuCode), shipmentItems);
            }
            for (String skuCode : existSkuCodeRemain) {
                updateShipmentItemRefundQuantity(skuCode, -(existSkuCodeAndQuantity.get(skuCode) == null ? 0 : existSkuCodeAndQuantity.get(skuCode)), shipmentItems);
            }
        }
    }

    //完善售后单仓库信息
    private void completeWareHoseAndExpressInfo(Shipment shipment,Integer refundType, RefundExtra refundExtra, EditSubmitRefundInfo submitRefundInfo) {

        //非仅退款则验证仓库是否有效、物流信息是否有效(因为仅退款不需要仓库信息)
        if (!Objects.equals(refundType, MiddleRefundType.AFTER_SALES_REFUND.value())) {

            //拒收
            if (Objects.equals(refundType, MiddleRefundType.REJECT_GOODS.value())){

                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                //填入售后单仓库id
                refundExtra.setWarehouseId(shipmentExtra.getWarehouseId());
                //填入售后单仓库名称
                refundExtra.setWarehouseName(shipmentExtra.getWarehouseName());
            } else {
                //根据默认售后仓规则填入的仓库id查询仓库信息
                Warehouse warehouse = findWarehouseById(submitRefundInfo.getWarehouseId());
                //填入售后单仓库id
                refundExtra.setWarehouseId(warehouse.getId());
                //填入售后单仓库名称
                refundExtra.setWarehouseName(warehouse.getName());
            }
            //填入物流编码，公司
            refundExtra.setShipmentCorpCode(submitRefundInfo.getShipmentCorpCode());
            //物流公司名称
            refundExtra.setShipmentCorpName(submitRefundInfo.getShipmentCorpName());
            //物流单号
            refundExtra.setShipmentSerialNo(submitRefundInfo.getShipmentSerialNo());
        } else{
            //仅退款的售后单默认退货仓是店铺配置的发货仓
            //Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(submitRefundInfo.getShipmentCode());
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
            String warehouseId = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID, openShop);
            String warehouseName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME, openShop);
            refundExtra.setWarehouseId(Long.valueOf(warehouseId));
            refundExtra.setWarehouseName(warehouseName);
        }
    }
    //完善云聚退货仓信息
    private void completeYunJUWareHoseAndExpressInfo(ShopOrder shopOrder,Integer refundType,RefundExtra refundExtra,EditSubmitRefundInfo submitRefundInfo){

            //非仅退款则验证仓库是否有效、物流信息是否有效
            if(!Objects.equals(refundType, MiddleRefundType.AFTER_SALES_REFUND.value())){
                String refundWarehouseId;
                String refundWarehouseName;
                if (StringUtils.isEmpty(submitRefundInfo.getReturnStockid())){ //为空取默认退货仓
                    OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopOrder.getShopId());
                    refundWarehouseId = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID, openShop);
                    refundWarehouseName = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME, openShop);
                    String refundWareHouseCode= orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.DEFAULT_REFUND_OUT_WAREHOUSE_CODE, openShop);

                    refundExtra.setReturnStockCode(refundWareHouseCode);

                 }else { //不为空

                    Warehouse warehouse = warehouseCacher.findByCode(submitRefundInfo.getReturnStockid());
                    refundWarehouseId=warehouse.getId()+"";
                    refundWarehouseName=warehouse.getName();
                }
                refundExtra.setWarehouseId(Long.valueOf(refundWarehouseId));
                refundExtra.setWarehouseName(refundWarehouseName);
                refundExtra.setShipmentCorpCode(submitRefundInfo.getShipmentCorpCode());
                refundExtra.setShipmentSerialNo(submitRefundInfo.getShipmentSerialNo());
                refundExtra.setShipmentCorpName(submitRefundInfo.getShipmentCorpName());
            }

    }
    /**
     * 完善换货商品信息
     *
     * @param refundItems      当前需要换货的商品
     * @param refundType       售后类型
     * @param submitRefundInfo 前端提交的售后商品
     * @param extraMap
     */
    private void completeChangeItemInfo(List<RefundItem> refundItems, Integer refundType, EditSubmitRefundInfo submitRefundInfo, Map<String, String> extraMap) {
        if (Objects.equals(MiddleRefundType.AFTER_SALES_CHANGE.value(), refundType)) {
            List<RefundItem> changeItems = makeChangeItemInfo(refundItems, submitRefundInfo);
            completeSkuAttributeInfo(changeItems);
            extraMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, mapper.toJson(Lists.newArrayList(changeItems)));
        }
    }


    /**
     * 换货商品信息
     * @param refundItems
     * @param submitRefundInfo
     * @return
     */
    private List<RefundItem> makeChangeItemInfo(List<RefundItem> refundItems, EditSubmitRefundInfo submitRefundInfo) {
        List<RefundItem> changeRefundItems = Lists.newArrayList();
        Map<String, RefundItem> skuCodeAndRefundItemsMap = Maps.newHashMap();
        refundItems.forEach(refundItem -> {
            skuCodeAndRefundItemsMap.put(refundItem.getSkuCode(), refundItem);
        });
        List<EditSubmitChangeItem> submitChangeItems = submitRefundInfo.getEditSubmitChangeItems();
        submitChangeItems.forEach(changeItem -> {
            RefundItem changeRefundItem = new RefundItem();
            changeRefundItem.setApplyQuantity(changeItem.getChangeQuantity());
            changeRefundItem.setAlreadyHandleNumber(0);
            changeRefundItem.setSkuCode(changeItem.getChangeSkuCode());
            //换货商品价格
            changeRefundItem.setSkuPrice(changeItem.getChangeSkuPrice());
            changeRefundItem.setCleanPrice(changeItem.getChangeSkuPrice());
            changeRefundItem.setCleanFee(changeItem.getChangeSkuPrice()*changeItem.getChangeQuantity());
            changeRefundItems.add(changeRefundItem);
        });

        return changeRefundItems;

    }

    private Warehouse findWarehouseById(Long warehouseId) {

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

    /**
     * 判断退货数量是否有效
     *
     * @param submitRefundInfo 从前端提交过来的请求参数
     * @param shipmentItems    对应发货单的发货商品的集合
     * @return
     */
    private List<RefundItem> checkRefundQuantity(EditSubmitRefundInfo submitRefundInfo, List<ShipmentItem> shipmentItems) {
        //退货商品的列表集合
        List<EditSubmitRefundItem> editSubmitRefundItems = submitRefundInfo.getEditSubmitRefundItems();
        //发货单的sku-quantity集合
        Map<String, Integer> skuCodesAndQuantity = Maps.newHashMap();
        //发货单skuCode集合列表
        List<String> skuCodes = Lists.newArrayList();
        //获取发货单skuCode-发货单商品的map集合
        Map<String, ShipmentItem> skuCodesAndShipmentItems = Maps.newHashMap();
        shipmentItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(), shipmentItem.getQuantity());
            skuCodesAndShipmentItems.put(shipmentItem.getSkuCode(), shipmentItem);
            skuCodes.add(shipmentItem.getSkuCode());
        });
        List<RefundItem> refundItems = Lists.newArrayList();
        int count = 0;
        //判断请求的skuCode在不在申请的发货单中，count>0代表存在skuCode不在发货单中
        List<String> invalidSkuCodes = Lists.newArrayList();
        for (EditSubmitRefundItem editSubmitRefundItem : editSubmitRefundItems) {
            if (skuCodes.contains(editSubmitRefundItem.getRefundSkuCode())) {
                //判断金额是否小于0
                if (editSubmitRefundItem.getRefundQuantity()<0){
                    log.error("refund applyQuantity:{}【 invalid",editSubmitRefundItem.getRefundQuantity());
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                //判断申请售后的商品数量和已经退货的商品数量之和是否大于发货单中商品的数量
                int currentRefundApplyQuantity = editSubmitRefundItem.getRefundQuantity();
                int alreadyRefundApplyQuantity = skuCodesAndShipmentItems.get(editSubmitRefundItem.getRefundSkuCode()).getRefundQuantity() == null ? 0 : skuCodesAndShipmentItems.get(editSubmitRefundItem.getRefundSkuCode()).getRefundQuantity();
                if ((currentRefundApplyQuantity + alreadyRefundApplyQuantity) > skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode())) {
                    log.error("refund applyQuantity:{} gt available applyQuantity:{}", editSubmitRefundItem.getRefundQuantity(), skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()));
                    throw new JsonResponseException("refund.apply.quantity.invalid");
                }
                RefundItem refundItem = new RefundItem();
                ShipmentItem shipmentItem = skuCodesAndShipmentItems.get(editSubmitRefundItem.getRefundSkuCode());
                BeanMapper.copy(shipmentItem, refundItem);
                //填入实际申请售后的数量
                refundItem.setApplyQuantity(editSubmitRefundItem.getRefundQuantity());
                //填入实际退款的单价金额
                refundItem.setFee(Long.valueOf(shipmentItem.getCleanFee()));
                refundItems.add(refundItem);
            } else {
                count++;
            }
        }
        if (count == 0) {
            return refundItems;
        } else {
            log.error("refund sku codes:{} invalid", invalidSkuCodes);
            throw new JsonResponseException("check.refund.applyQuantity.fail");
        }
    }

    /**
     *
     * 判断退货数量是否有效
     * @param submitRefundInfo 从前端提交过来的请求参数
     * @param shipmentItems    对应发货单的发货商品的集合
     * @return
     */
    private List<RefundItem> checkYunJuRefundQuantity(EditSubmitRefundInfo submitRefundInfo,List<ShipmentItem> shipmentItems){
        //退货商品的列表集合
        List<EditSubmitRefundItem> editSubmitRefundItems = submitRefundInfo.getEditSubmitRefundItems();
        //发货单的sku-quantity集合
        Map<String, Integer> skuCodesAndQuantity = Maps.newHashMap();
        //发货单skuCode集合列表
        List<String> skuCodes = Lists.newArrayList();
        //获取发货单skuCode-发货单商品的map集合
        Map<String, ShipmentItem> skuCodesAndShipmentItems = Maps.newHashMap();
        shipmentItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(), shipmentItem.getQuantity());
            skuCodesAndShipmentItems.put(shipmentItem.getSkuCode(), shipmentItem);
            skuCodes.add(shipmentItem.getSkuCode());
        });
        List<RefundItem> refundItems = Lists.newArrayList();
        int count = 0;
        //判断请求的skuCode在不在申请的发货单中，count>0代表存在skuCode不在发货单中
        List<String> invalidSkuCodes = Lists.newArrayList();
        for (EditSubmitRefundItem editSubmitRefundItem:editSubmitRefundItems){
            if (skuCodes.contains(editSubmitRefundItem.getRefundSkuCode())){
                if(!Objects.isNull(editSubmitRefundItem.getRefundQuantity())){
                    //判断金额是否小于0
                    if (editSubmitRefundItem.getRefundQuantity()<0){
                        log.error("refund applyQuantity:{}【 invalid",editSubmitRefundItem.getRefundQuantity());
                        throw new JsonResponseException("refund.apply.quantity.invalid");
                    }
                    //判断申请售后的商品数量是否大于发货单中商品的数量
                    if (editSubmitRefundItem.getRefundQuantity()>skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode())){
                        log.error("refund applyQuantity:{} gt available applyQuantity:{}",editSubmitRefundItem.getRefundQuantity(),skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()));
                        throw new JsonResponseException("refund.apply.quantity.invalid");
                    }
                }else { //云聚没传的话
                    editSubmitRefundItem.setRefundQuantity(skuCodesAndQuantity.get(editSubmitRefundItem.getRefundSkuCode()));
                }

                RefundItem refundItem = new RefundItem();
                ShipmentItem shipmentItem = skuCodesAndShipmentItems.get(editSubmitRefundItem.getRefundSkuCode());
                BeanMapper.copy(shipmentItem, refundItem);
                //填入实际申请售后的数量
                refundItem.setApplyQuantity(editSubmitRefundItem.getRefundQuantity());

                refundItem.setFee(editSubmitRefundItem.getFee()); //以云聚为准
                refundItem.setSkuAfterSaleId(editSubmitRefundItem.getSkuAfterSaleId());//子退货单号 yunju add
                refundItems.add(refundItem);
            } else {
                count++;
            }
        }
        if (count == 0) {
            return refundItems;
        } else {
            log.error("refund sku codes:{} invalid", invalidSkuCodes);
            throw new JsonResponseException("check.refund.applyQuantity.fail");
        }
    }

    //更新发货单商品中的已退货数量
    private void updateShipmentItemRefundQuantity(String skuCode, Integer refundQuantity, List<ShipmentItem> shipmentItems) {
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (Objects.equals(skuCode, shipmentItem.getSkuCode())) {
                shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) + refundQuantity);
            }
        }
    }

    /**
     * 获取货品条码的详细信息
     *
     * @param refundItems
     */
    private void completeSkuAttributeInfo(List<RefundItem> refundItems) {

        //获取sku货品条码的集合
        List<String> skuCodes = refundItems.stream().filter(Objects::nonNull).map(RefundItem::getSkuCode).collect(Collectors.toList());
        //根据货品条码查询条码的详细信息
        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(skuCodes);
        if (!skuTemplateRes.isSuccess()) {
            log.error("find sku template by sku skuCodes:{} fail,error:{}", skuCodes, skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        //获取货品条码，货品条码信息的map集合，但是要过滤掉已经删除的条码
        Map<String, SkuTemplate> skuCodeAndTemplateMap = skuTemplateRes.getResult().stream().filter(Objects::nonNull).filter(it -> !Objects.equals(it.getStatus(), -3))
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));
        if (skuCodeAndTemplateMap.size() == 0) {
            throw new JsonResponseException("sku.may.be.deleted");
        }
        refundItems.forEach(it -> {
            SkuTemplate skuTemplate = skuCodeAndTemplateMap.get(it.getSkuCode());
            it.setAttrs(skuTemplate.getAttrs());
            it.setSkuName(skuTemplate.getName());
        });
    }

    /**
     * 售后单取消,删除时回滚发货单中退货数量
     *
     * @param refund 售后单
     */
    public void rollbackRefundQuantities(Refund refund) {
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String, RefundItem> skuCodeAndRefundItemMap = refundItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(RefundItem::getSkuCode, it -> it));

        shipmentItems.forEach(it -> {
            RefundItem refundItem = skuCodeAndRefundItemMap.get(it.getSkuCode());
            if (refundItem != null) {
                it.setRefundQuantity(it.getRefundQuantity() - refundItem.getApplyQuantity());
            }

        });
        Map<String, String> shipmentExtraMap = shipment.getExtra();
        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(shipmentItems));
        shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);
    }


    /**
     * 售后单客服备注
     *
     * @param refundId            售后单主键
     * @param customerServiceNote 客服备注
     */
    public void addCustomerServiceNote(long refundId, String customerServiceNote) {

        Response<Boolean> response = refundWriteService.updateSellerNote(refundId, customerServiceNote);
        if (!response.isSuccess()) {
            log.error("refund add customerServiceNote failed,refundId is({}),caused by{}", refundId, response.getError());
            throw new JsonResponseException("add customer service note fail");
        }
    }


    /**
     * 创建丢件补发类型类型的逆向单
     *
     * @param submitRefundInfo 参数实体
     * @return
     */
    public Long createRefundForLost(SubmitRefundInfo submitRefundInfo) {
        //获取订单
//        ShopOrder shopOrder = orderReadLogic.findShopOrderById(submitRefundInfo.getOrderId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(submitRefundInfo.getOrderCode());
        //获取订单下或者售后单的所有有效发货单
        List<ShipmentItem> shipmentItems = Lists.newArrayList();
        if (Objects.equals(submitRefundInfo.getReleOrderType(),1)){
            shipmentItems = shipmentReadLogic.findShopOrderShipmentItems(shopOrder.getOrderCode());
        }else{
            shipmentItems = shipmentReadLogic.findAfterSaleShipmentItems(submitRefundInfo.getReleOrderNo());
        }
        //申请数量是否有效
        List<RefundItem> refundItems = checkChangeItemsForLost(submitRefundInfo, shipmentItems);
        //完善售后货品信息
        completeSkuAttributeInfo(refundItems);
        //新建售后单参数组装
        Refund refund = new Refund();
        refund.setBuyerId(shopOrder.getBuyerId());
        refund.setBuyerName(shopOrder.getBuyerName());
        refund.setBuyerNote(submitRefundInfo.getBuyerNote());
        refund.setRefundAt(new Date());
        if (Objects.equals(submitRefundInfo.getOperationType(), 1)) {
            refund.setStatus(MiddleRefundStatus.WAIT_HANDLE.getValue());
        } else {
            refund.setStatus(MiddleRefundStatus.LOST_WAIT_CREATE_SHIPMENT.getValue());
        }
        refund.setShopId(shopOrder.getShopId());
        refund.setShopName(shopOrder.getShopName());
        refund.setFee(shopOrder.getFee());
        refund.setRefundType(submitRefundInfo.getRefundType());
        Map<String, String> extraMap = Maps.newHashMap();
        RefundExtra refundExtra = new RefundExtra();
        //关联单号
        refundExtra.setReleOrderNo(submitRefundInfo.getReleOrderNo());
        //关联单号类型
        refundExtra.setReleOrderType(submitRefundInfo.getReleOrderType());
        //关联单号
        refund.setReleOrderCode(submitRefundInfo.getReleOrderNo());
        //获取订单下的任意发货单
        List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
        Shipment shipment = shipments.stream().filter(Objects::nonNull).filter(it->(!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())&&!Objects.equals(it.getStatus(),MiddleShipmentsStatus.REJECTED.getValue()))).findAny().get();
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(), ReceiverInfo.class);
        refundExtra.setReceiverInfo(receiverInfo);
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        extraMap.put(TradeConstants.REFUND_LOST_ITEM_INFO, mapper.toJson(refundItems));
        //完善丢件补发地址信息
        if (Objects.equals(MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value(), submitRefundInfo.getRefundType())) {
            if (Objects.nonNull(submitRefundInfo.getMiddleChangeReceiveInfo())) {
                extraMap.put(TradeConstants.MIDDLE_CHANGE_RECEIVE_INFO, mapper.toJson(submitRefundInfo.getMiddleChangeReceiveInfo()));
            }
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
        refund.setExtra(extraMap);
        //打标
        Map<String, String> tagMap = Maps.newHashMap();
        tagMap.put(TradeConstants.REFUND_SOURCE, String.valueOf(RefundSource.MANUAL.value()));
        refund.setTags(tagMap);
        //创建售后单
        Response<Long> rRefundRes = middleRefundWriteService.create(refund, Lists.newArrayList(shopOrder.getId()), OrderLevel.SHOP);
        if (!rRefundRes.isSuccess()) {
            log.error("failed to create {}, error code:{}", refund, rRefundRes.getError());
            throw new JsonResponseException(rRefundRes.getError());
        }
        return rRefundRes.getResult();
    }

    /**
     * 丢件补发参数组装
     *
     * @param submitRefundInfo
     * @param shipmentItems    该订单下的所有的商品集合
     * @return
     */
    private List<RefundItem> checkChangeItemsForLost(EditSubmitRefundInfo submitRefundInfo, List<ShipmentItem> shipmentItems) {
        //可能存在只有部分商品被弄丢了，这个时候需要传输商品条码
        //传输过来的需要丢件补发的商品集合
        List<ShipmentItem> lostItems = submitRefundInfo.getLostItems();
        //获取需要丢件补发的skuCode的集合
        List<String> changeSkuCodes = lostItems.stream().filter(Objects::nonNull).map(ShipmentItem::getSkuCode).collect(Collectors.toList());
        //获取sku以及丢件补发数量的集合
        List<RefundItem> refundItems = Lists.newArrayList();
        Map<String, Integer> skuCodesAndQuantity = Maps.newHashMap();
        lostItems.forEach(shipmentItem -> {
            skuCodesAndQuantity.put(shipmentItem.getSkuCode(), shipmentItem.getQuantity());
        });
        shipmentItems.forEach(shipmentItem -> {
            //获取所有需要补发的RefundItem
            if (changeSkuCodes.contains(shipmentItem.getSkuCode())) {
                //要限制丢件补发的数量，获取丢件补发传入的数量
                int lostQuantity = skuCodesAndQuantity.get(shipmentItem.getSkuCode()) == null ? 0 : skuCodesAndQuantity.get(shipmentItem.getSkuCode());
                if (lostQuantity > shipmentItem.getQuantity()) {
                    throw new JsonResponseException("lost.refund.quantity.can.not.larger.than.shipment.quanity");
                }
                RefundItem refundItem = new RefundItem();
                BeanMapper.copy(shipmentItem, refundItem);
                //填入售后申请数量
                refundItem.setApplyQuantity(skuCodesAndQuantity.get(shipmentItem.getSkuCode()));
                //填入货品条码
                refundItem.setSkuCode(shipmentItem.getSkuCode());
                refundItems.add(refundItem);
            }
        });
        return refundItems;
    }

    /**
     * 丢件补发类型的售后单完善
     *
     * @param refund
     * @param submitRefundInfo
     */
    public void completeHandleForLostType(Refund refund, EditSubmitRefundInfo submitRefundInfo) {
        //获取售后单extra信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //获取售后单订单关联信息
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        //获取订单
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        //获取订单或者售后单的所有有效发货单
        List<ShipmentItem> shipmentItems = Lists.newArrayList();
        if (Objects.equals(submitRefundInfo.getReleOrderType(),1)){
            shipmentItems = shipmentReadLogic.findShopOrderShipmentItems(shopOrder.getOrderCode());
        }else{
            shipmentItems = shipmentReadLogic.findAfterSaleShipmentItems(submitRefundInfo.getReleOrderNo());
        }
        //获取最新的丢件补发的商品
        List<RefundItem> cureentLostRefundItems = checkChangeItemsForLost(submitRefundInfo, shipmentItems);
        //获取货品条码的详细信息
        completeSkuAttributeInfo(cureentLostRefundItems);

        Map<String, String> extraMap = refund.getExtra();
        //添加处理完成时间
        refundExtra.setHandleDoneAt(new Date());
        //更新售后单的相关信息
        Refund updateRefund = new Refund();
        updateRefund.setId(refund.getId());
        updateRefund.setBuyerNote(submitRefundInfo.getBuyerNote());
        updateRefund.setFee(shopOrder.getFee());
        //更新售后单的extra信息
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        //更新售后单的货品信息
        extraMap.put(TradeConstants.REFUND_LOST_ITEM_INFO, mapper.toJson(cureentLostRefundItems));
        //提交动作
        if (Objects.equals(submitRefundInfo.getOperationType(), 2)) {
            //更新售后单状态
            Response<Boolean> updateStatusRes = updateStatusLocking(refund, MiddleOrderEvent.LOST_HANDLE.toOrderOperation());
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status to:{} fail,error:{}", refund.getId(), updateStatusRes.getError());
                throw new JsonResponseException(updateStatusRes.getError());
            }
        }
        //表明售后单的信息已经全部完善
        extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
        updateRefund.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(updateRefund);
        if (!updateRes.isSuccess()) {
            log.error("update refund:{} fail,error:{}", updateRefund, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    /**
     * 丢件补发类型的售后单的删除
     *
     * @param refund 售后单
     */
    public void deleteRefundForLost(Refund refund) {
        //判断类型
        RefundSource refundSource = refundReadLogic.findRefundSource(refund);
        if (Objects.equals(refundSource.value(), RefundSource.THIRD.value())) {
            log.error("refund(id:{}) is third party refund  so cant not delete", refund.getId());
            throw new JsonResponseException("third.party.refund.can.not.delete");
        }
        //更新状态
        Response<Boolean> updateRes = this.updateStatusLocking(refund, MiddleOrderEvent.DELETE.toOrderOperation());
        if (!updateRes.isSuccess()) {
            log.error("delete refund(id:{}) fail,error:{}", refund.getId(), updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    /**
     * 获取苏宁天猫的退款结果
     *
     * @param refund
     */
    public void getThirdRefundResult(Refund refund) {
        String outId = refund.getOutId();
        if (StringUtils.hasText(outId) && outId.contains("taobao")) {
            String channel = refundReadLogic.getOutChannelTaobao(outId);
            if (Objects.equals(channel, MiddleChannel.TAOBAO.getValue())
                    && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
                Refund newRefund = refundReadLogic.findRefundById(refund.getId());
                TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
                event.setRefundId(refund.getId());
                event.setChannel(channel);
                event.setOpenShopId(newRefund.getShopId());
                event.setOpenAfterSaleId(refundReadLogic.getOutafterSaleIdTaobao(outId));
                //保证异步任务的可靠性，将原来的eventBus方式改用新的基于定时任务执行
                //eventBus.post(event);
                this.createRefundResultTask(event);
            }
        }
        //如果是苏宁的售后单，将会主动查询售后单的状态
        if (StringUtils.hasText(outId) && outId.contains("suning")&&!outId.contains("suning-sale")) {
            String channel = refundReadLogic.getOutChannelSuning(outId);
            if (Objects.equals(channel, MiddleChannel.TAOBAO.getValue())
                    && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
                Refund newRefund = refundReadLogic.findRefundById(refund.getId());
                OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
                TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
                event.setRefundId(refund.getId());
                event.setChannel(channel);
                event.setOpenShopId(newRefund.getShopId());
                event.setOpenOrderId(shopOrder.getOutId());
                //保证异步任务的可靠性，将原来的eventBus方式改用新的基于定时任务执行
                //eventBus.post(event);
                this.createRefundResultTask(event);
            }
        }

        if(StringUtils.hasText(outId) && outId.contains("suning-sale")) {
            String channel = MiddleChannel.SUNINGSALE.getValue();
            if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
                Refund newRefund = refundReadLogic.findRefundById(refund.getId());

                TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
                event.setRefundId(refund.getId());
                event.setChannel(channel);
                event.setOpenShopId(newRefund.getShopId());
                event.setOpenOrderId(refundReadLogic.getOutSkuOrderIdSuningSale(outId));
                eventBus.post(event);

            }
    }

        //如果是云聚的售后单,将主动查询售后单的状态
        if (StringUtils.hasText(outId) && outId.contains("YJ")) {
            log.info(" refund={}",refund);
            String channel = refundReadLogic.getOutChannelSuning(outId);
            if (Objects.equals(channel, MiddleChannel.YJ.getValue())
                    && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
                Refund newRefund = refundReadLogic.findRefundById(refund.getId());
                OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
                TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
                event.setRefundId(refund.getId());
                event.setChannel(channel);
                event.setOpenShopId(newRefund.getShopId());
                event.setOpenOrderId(shopOrder.getOutId());
                eventBus.post(event);
            }
        }
    }



    /**
     * @Description 查询第三方（天猫、苏宁）售后单确认状态任务创建
     * @Date        2018/5/31
     * @param       event
     * @return
     */
    private void createRefundResultTask(TaobaoConfirmRefundEvent event){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.THIRD_REFUND_RESULT.toString());
        biz.setContext(mapper.toJson(event));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }

    /**
     * 获取两个集合的交集
     *
     * @param list1
     * @param list2
     * @return
     */
    public List<String> getDntersectionList(List<String> list1, List<String> list2) {
        Set<String> sets = Sets.newHashSet(list1);
        Set<String> sets2 = Sets.newHashSet(list2);
        SetView<String> intersection = Sets.intersection(sets, sets2);
        return new ArrayList<>(intersection);
    }

    /**
     * 获取两个集合的差集
     *
     * @param list1
     * @param list2
     * @return
     */
    public List<String> getDifferenceList(List<String> list1, List<String> list2) {
        Set<String> sets = Sets.newHashSet(list1);
        Set<String> sets2 = Sets.newHashSet(list2);
        SetView<String> intersection = Sets.difference(sets, sets2);
        return new ArrayList<>(intersection);
    }

    /**
     * 售后单分配售后金额
     *
     * @param refundItems
     * @param fee
     * @return
     */
    private List<RefundItem> calcRefundItemFees(List<RefundItem> refundItems, Long fee) {
        Long totalRefundAmount = 0L;
        for (RefundItem refundItem : refundItems) {
            totalRefundAmount = totalRefundAmount + refundItem.getCleanPrice() * refundItem.getApplyQuantity();
        }
        //计算比例
        for (RefundItem refundItem : refundItems) {
            if (totalRefundAmount>0L){
                Long itemFee = ((refundItem.getCleanPrice() * refundItem.getApplyQuantity() * fee) / totalRefundAmount);
                refundItem.setFee(itemFee);
            }
        }
        return refundItems;
    }

    /**
     *
     * @param shopId
     */
    public void refundAmountOrigin(Long shopId){
        int pageNo = 1;
        while(true){
            MiddleRefundCriteria criteria = new MiddleRefundCriteria();
            criteria.setShopId(shopId);
            criteria.setPageNo(pageNo);
            Response<Paging<RefundPaging>> response = refundReadLogic.refundPaging(criteria);
            if (!response.isSuccess()) {
                log.error("find refund by criteria:{} fail,error:{}", criteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<RefundPaging> refundPagings = response.getResult().getData();
            if (refundPagings.isEmpty()){
                log.info("all refunds done pageNo is {}",pageNo);
                break;
            }
            for (RefundPaging refundPaging:refundPagings){
                try {

                    Refund refund = refundPaging.getRefund();
                    if (refund.getStatus()<0){
                        log.info("shipment status <0");
                        continue;
                    }
                    if (Objects.equals(refund.getRefundType(),4)){
                        continue;
                    }
                    if (Objects.equals(refund.getRefundType(),3)){
                        continue;
                    }
                    SycHkRefund sycHkRefund = syncRefundLogic.makeSyncHkRefund(refund);
                    List<SycHkRefundItem> items = syncRefundLogic.makeSycHkRefundItemList(refund);
                    for (SycHkRefundItem sycHkRefundItem:items){
                        try{
                            RefundAmount refundAmount = new RefundAmount();
                            refundAmount.setRefundNo(sycHkRefund.getRefundNo());
                            refundAmount.setOrderNo(sycHkRefund.getOrderNo());
                            refundAmount.setShopId(sycHkRefund.getShopId());
                            refundAmount.setPerformanceShopId(sycHkRefund.getPerformanceShopId());
                            refundAmount.setStockId(sycHkRefund.getStockId());
                            refundAmount.setRefundOrderAmount(sycHkRefund.getRefundOrderAmount());
                            if (Objects.equals(refund.getRefundType(),1)){
                                refundAmount.setType("仅退款");
                            }
                            if (Objects.equals(refund.getRefundType(),2)){
                                refundAmount.setType("退货退款");
                            }
                            if (Objects.equals(refund.getRefundType(),3)){
                                refundAmount.setType("换货");
                            }
                            refundAmount.setTotalRefund(sycHkRefund.getTotalRefund());
                            refundAmount.setOnlineOrderNo(sycHkRefundItem.getOnlineOrderNo());
                            Map<String,String> extraMap = refund.getExtra();
                            String hkRefundId = extraMap.get(TradeConstants.HK_REFUND_ID);
                            //恒康单号
                            refundAmount.setHkOrderNo(hkRefundId);
                            //pos单号
                            try{
                                Response<PoushengSettlementPos> sR = poushengSettlementPosReadService.findByRefundIdAndPosType(refund.getId(),2);
                                if(!sR.isSuccess()){
                                    log.error("find pos failed");
                                }
                                PoushengSettlementPos poushengSettlementPos = sR.getResult();
                                refundAmount.setPosNo(poushengSettlementPos.getPosSerialNo());
                            }catch (Exception e){
                                log.error("find.pos.failed,caused by {}",e.getMessage());
                            }
                            refundAmount.setRefundSubNo(sycHkRefundItem.getRefundSubNo());
                            refundAmount.setOrderSubNo(sycHkRefundItem.getOrderSubNo());
                            refundAmount.setBarCode(sycHkRefundItem.getBarCode());
                            refundAmount.setItemNum(String.valueOf(sycHkRefundItem.getItemNum()));
                            refundAmount.setSalePrice(sycHkRefundItem.getSalePrice());
                            refundAmount.setRefundAmount(sycHkRefundItem.getRefundAmount());
                            Response<Long> r = refundAmountWriteService.create(refundAmount);
                            if (!r.isSuccess()){
                                log.error("create refund amount failed,shipment id is {},barCode is {}",refund.getId(),sycHkRefundItem.getBarCode());
                            }
                        }catch (Exception e){
                            log.error("create refund amount failed,shipment id is {},barCode is {}",refund.getId(),sycHkRefundItem.getBarCode());
                        }
                    }
                }catch (Exception e){
                    log.error("refundAmountOrigin shop id:{} fail,cause:{}",shopId, Throwables.getStackTraceAsString(e));
                }
            }
            pageNo++;
        }
    }

    public void updateRefundInfos(Long shopId){
        int pageNo = 1;
        while(true){
            MiddleRefundCriteria criteria = new MiddleRefundCriteria();
            criteria.setShopId(shopId);
            criteria.setPageNo(pageNo);
            Response<Paging<RefundPaging>> response = refundReadLogic.refundPaging(criteria);
            if (!response.isSuccess()) {
                log.error("find refund by criteria:{} fail,error:{}", criteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<RefundPaging> refundPagings = response.getResult().getData();
            if (refundPagings.isEmpty()){
                log.info("all refunds done pageNo is {}",pageNo);
                break;
            }
            for (RefundPaging refundPaging:refundPagings){
                try {
                    Refund refund = refundPaging.getRefund();
                    if (refund.getStatus()<0){
                        log.info("shipment status <0");
                        continue;
                    }
                    if (Objects.equals(refund.getRefundType(),4)){
                        continue;
                    }
                    if (Objects.equals(refund.getRefundType(),3)){
                        continue;
                    }
                    RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
                    List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
                    List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
                    for (RefundItem refundItem :refundItems){
                        for (ShipmentItem shipmentItem:shipmentItems){
                            if (Objects.equals(refundItem.getSkuCode(),shipmentItem.getSkuCode())){
                                refundItem.setCleanPrice(shipmentItem.getCleanPrice());
                                refundItem.setCleanFee(shipmentItem.getCleanFee());
                            }
                        }
                    }
                    Long refundFee = 0L;
                    for (RefundItem refundItem:refundItems){
                        refundFee = refundFee+(refundItem.getCleanPrice()*refundItem.getApplyQuantity());
                    }
                    //算出来的金额大于申请的金额，以申请的金额为准
                    if (refundFee>=refund.getFee()){
                       refundItems =  calcRefundItemFees(refundItems,refund.getFee());
                    }else{
                        refundItems = calcRefundItemFees(refundItems,refundFee);
                        refund.setFee(refundFee);
                    }
                    Map<String,String> extraMap = refund.getExtra();
                    extraMap.put(TradeConstants.REFUND_ITEM_INFO,mapper.toJson(refundItems));
                    refund.setExtra(extraMap);
                    this.update(refund);
                }catch (Exception e){
                    log.error("updateRefundInfos shop id:{} fail,cause:{}",shopId, Throwables.getStackTraceAsString(e));
                }
            }
            pageNo++;
        }
    }

    public Response<Boolean> cancelRefund(Refund refund, OpenClientAfterSale afterSale){
        //如果这个时候拉取过来的售后单是用户自己取消且为退货类型的可以更新售后单的状态
        if (afterSale.getStatus() == OpenClientAfterSaleStatus.RETURN_CLOSED
                && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
            //判断售后单状态
            Flow flow = flowPicker.pickAfterSales();
            //这个时候的状态可能为待完善,待同步恒康,同步恒康失败
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation())
                    || flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.SYNC_HK.toOrderOperation())) {
                //直接售后单的状态为已取消即可
                Response<Boolean> updateR = refundWriteService.updateStatus(refund.getId(), MiddleRefundStatus.CANCELED.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {}cause:{}",
                            refund.getId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                    return Response.fail(updateR.getError());
                } else {
                    //回滚发货单的数量
                    this.rollbackRefundQuantities(refund);
                }
                return Response.ok();
            }
            //已经同步恒康
            if (flow.operationAllowed(refund.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                    return Response.fail(syncRes.getError());
                } else {
                    //回滚发货单的数量
                    this.rollbackRefundQuantities(refund);
                }
                return Response.ok();
            }
        }

        log.error("cancel refund(id:{}) fail,because after sale:{}",refund.getId(),afterSale);
        return Response.fail("refund.status.invalid");
    }
}
