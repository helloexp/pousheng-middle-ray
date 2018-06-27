package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.hksyc.component.SycHkRefundOrderApi;
import com.pousheng.middle.hksyc.dto.HkResponseHead;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefund;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundItem;
import com.pousheng.middle.hksyc.dto.trade.SycHkRefundResponseBody;
import com.pousheng.middle.hksyc.dto.trade.SycRefundResponse;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.HkRefundType;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiReturnLogic;
import com.pousheng.middle.yyedisyc.component.SycYYEdiRefundCancelApi;
import com.pousheng.middle.yyedisyc.component.SycYYEdiRefundOrderApi;
import com.pousheng.middle.yyedisyc.dto.YYEdiResponse;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiReturnInfo;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 同步恒康逆向订单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundLogic {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycHkRefundOrderApi sycHkRefundOrderApi;
    @Autowired
    private SycHkOrderCancelApi sycHkOrderCancelApi;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private SyncYYEdiReturnLogic syncYYEdiReturnLogic;
    @Autowired
    private SycYYEdiRefundOrderApi sycYYEdiRefundOrderApi;
    @Autowired
    private SycYYEdiRefundCancelApi sycYYEdiRefundCancelApi;


    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步退货单到skx和yyedi
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundToSkxAndYYedi(Refund refund) {
        log.info("sync refund(id:{}) to skx and yyedi",refund.getId());

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //skx退货单同步skx是否成功
        Boolean skxRefundSyncSkxResult = Arguments.isNull(refundExtra.getSkxRefundSyncSkxResult())?Boolean.FALSE:refundExtra.getSkxRefundSyncSkxResult();
        //skx退货单同步YYedi是否成功
        Boolean skxRefundSyncYyediResult = Arguments.isNull(refundExtra.getSkxRefundSyncYyediResult())?Boolean.FALSE:refundExtra.getSkxRefundSyncYyediResult();;

        Flow flow = flowPicker.pickAfterSales();
        Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
        refund.setStatus(targetStatus);

        String syncErrorMsg="";
        //同步skx
        if(!skxRefundSyncSkxResult){
            try {
                //要根据不同步的售后单类型来决定同步成功或失败的状态
                String response = sycHkRefundOrderApi.doSyncRefundOrder(this.makeSyncHkRefund(refund), this.makeSycHkRefundItemList(refund));
                SycRefundResponse sycRefundResponse =JsonMapper.nonEmptyMapper().fromJson(response,SycRefundResponse.class);
                HkResponseHead head = sycRefundResponse.getHead();
                if (Objects.equals(head.getCode(), "0")) {
                    skxRefundSyncSkxResult = Boolean.TRUE;
                    //更新标记位和skx的退货单号
                    refundExtra.setSkxRefundSyncSkxResult(Boolean.TRUE);
                    Refund update = new Refund();
                    update.setId(refund.getId());
                    SycHkRefundResponseBody body = sycRefundResponse.getRefundBody();
                    Map<String,String> extraMap = refund.getExtra();
                    extraMap.put(TradeConstants.HK_REFUND_ID, String.valueOf(body.getErpOrderNo()));
                    extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    update.setExtra(extraMap);
                    Response<Boolean> updateRes =  refundWriteLogic.update(update);
                    if(!updateRes.isSuccess()){
                        log.error("update refund:{} fail,error:{}",update,updateRes.getError());
                    }
                } else {
                    skxRefundSyncSkxResult = Boolean.FALSE;
                    //更新同步状态
                    updateRefundSyncResult(refund, Boolean.FALSE,skxRefundSyncYyediResult);
                    syncErrorMsg = "同步失败，SKX返回信息:"+head.getMessage();
                    log.error(syncErrorMsg);
                }
            } catch (Exception e) {
                log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
                skxRefundSyncSkxResult = Boolean.FALSE;
                //更新同步状态
                updateRefundSyncResult(refund,Boolean.FALSE,skxRefundSyncYyediResult);
                syncErrorMsg = "同步SKX失败";

            }
        }



        //同步yyedi
        if(!skxRefundSyncYyediResult){
            try {
                //要根据不同步的售后单类型来决定同步成功或失败的状态
                YYEdiReturnInfo returnInfo =syncYYEdiReturnLogic.makeSyncYYEdiRefund(refund);
                String response = sycYYEdiRefundOrderApi.doSyncRefundOrder(Lists.newArrayList(returnInfo));
                YYEdiResponse yyEdiResponse =JsonMapper.nonEmptyMapper().fromJson(response,YYEdiResponse.class);
                if (Objects.equals(yyEdiResponse.getErrorCode(),TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)){
                    skxRefundSyncYyediResult = Boolean.TRUE;
                    //更新标记位
                    refundExtra.setSkxRefundSyncYyediResult(Boolean.TRUE);
                    Refund update = new Refund();
                    update.setId(refund.getId());
                    Map<String,String> extraMap = refund.getExtra();
                    extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    update.setExtra(extraMap);
                    Response<Boolean> updateRes =  refundWriteLogic.update(update);
                    if(!updateRes.isSuccess()){
                        log.error("update refund:{} fail,error:{}",update,updateRes.getError());
                    }
                }else{
                    skxRefundSyncYyediResult = Boolean.FALSE;
                    //更新同步状态
                    updateRefundSyncResult(refund,skxRefundSyncSkxResult,Boolean.FALSE);
                    syncErrorMsg = "订单派发中心返回信息:"+yyEdiResponse.getDescription();
                    log.error(syncErrorMsg);
                }
            } catch (Exception e) {
                log.error("sync yyedi refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
                //更新同步状态
                updateRefundSyncResult(refund,skxRefundSyncSkxResult,Boolean.FALSE);
                syncErrorMsg = "同步YYEDI失败";
            }
        }

        if(skxRefundSyncSkxResult&&skxRefundSyncYyediResult){
            //同步调用成功后，更新售后单的状态，及冗余恒康售后单号
            OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            return Response.ok(Boolean.TRUE);
        }else {
            updateRefundSyncFial(refund);
            return Response.fail(syncErrorMsg);
        }
    }


    /**
     * 同步恒康退货单
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundToHk(Refund refund) {
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        try {
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
            refund.setStatus(targetStatus);

            //要根据不同步的售后单类型来决定同步成功或失败的状态
            String response = sycHkRefundOrderApi.doSyncRefundOrder(this.makeSyncHkRefund(refund), this.makeSycHkRefundItemList(refund));
            SycRefundResponse sycRefundResponse =JsonMapper.nonEmptyMapper().fromJson(response,SycRefundResponse.class);
            HkResponseHead head = sycRefundResponse.getHead();
            if (Objects.equals(head.getCode(), "0")) {
                //同步调用成功后，更新售后单的状态，及冗余恒康售后单号
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(refund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
                refundWriteLogic.getThirdRefundResult(refund);

                Refund update = new Refund();
                update.setId(refund.getId());
                SycHkRefundResponseBody body = sycRefundResponse.getRefundBody();
                Map<String,String> extraMap = refund.getExtra();
                extraMap.put(TradeConstants.HK_REFUND_ID, String.valueOf(body.getErpOrderNo()));
                update.setExtra(extraMap);

                return refundWriteLogic.update(update);
            } else {
                //更新同步状态
                updateRefundSyncFial(refund);
                return Response.fail("恒康返回信息:"+head.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
            //更新同步状态
            updateRefundSyncFial(refund);
            return Response.fail("sync.hk.refund.fail");
        }


    }


    private void updateRefundSyncResult(Refund refund,Boolean skxRefundSyncSkxResult,Boolean skxRefundSyncYyediResult){

        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Refund toUpdate = new Refund();
        toUpdate.setId(refund.getId());
        refundExtra.setSkxRefundSyncSkxResult(skxRefundSyncSkxResult);
        refundExtra.setSkxRefundSyncYyediResult(skxRefundSyncYyediResult);
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        toUpdate.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteLogic.update(toUpdate);
        if (!updateRes.isSuccess()) {
            log.error("refund(id:{}) refund extra :{} fail,error:{}", refund.getId(), refundExtra, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

    }


    private void updateRefundCancelSyncResult(Refund refund,Boolean skxRefundCancelSyncSkxResult,Boolean skxRefundCancelSyncYyediResult){

        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Refund toUpdate = new Refund();
        toUpdate.setId(refund.getId());
        refundExtra.setSkxRefundCancelSyncSkxResult(skxRefundCancelSyncSkxResult);
        refundExtra.setSkxRefundCancelSyncYyediResult(skxRefundCancelSyncYyediResult);
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        toUpdate.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteLogic.update(toUpdate);
        if (!updateRes.isSuccess()) {
            log.error("refund(id:{}) refund extra :{} fail,error:{}", refund.getId(), refundExtra, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

    }

    private void updateRefundSyncFial(Refund refund){
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

    //获取同步成功事件
    private OrderOperation  getSyncSuccessOperation(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        if (Arguments.isNull(middleRefundType)) {
            log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
            throw new JsonResponseException("refund.type.invalid");
        }

        switch (middleRefundType) {
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.SYNC_RETURN_SUCCESS.toOrderOperation();
            case AFTER_SALES_REFUND:
                return MiddleOrderEvent.SYNC_REFUND_SUCCESS.toOrderOperation();
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.SYNC_CHANGE_SUCCESS.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }

    }


    /**
     * 同步恒康退货单取消
     *
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncRefundCancelToHk(Refund refund) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

            String response = sycHkOrderCancelApi.doCancelOrder(shipmentExtra.getErpOrderShopCode(), refund.getRefundCode(), 0,1);
            SycRefundResponse sycRefundResponse  = JsonMapper.nonEmptyMapper().fromJson(response,SycRefundResponse.class);
            HkResponseHead head = sycRefundResponse.getHead();
            if (Objects.equals(head.getCode(),"0")) {
                //同步调用成功后，更新售后单的状态
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            } else {
                //同步调用成功后，更新售后单的状态
                Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                return Response.fail("恒康返回信息:"+head.getMessage());
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            //同步调用成功后，更新售后单的状态
            Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("sync.hk.refund.fail");
        }
    }


    public Response<Boolean> syncRefundCancelToSkxAndYYedi(Refund refund){
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
        if (!updateStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //skx退货单取消同步skx是否成功
        Boolean skxRefundCancelSyncSkxResult = Arguments.isNull(refundExtra.getSkxRefundCancelSyncSkxResult()) ? Boolean.FALSE : refundExtra.getSkxRefundCancelSyncSkxResult();
        //skx退货单取消同步YYedi是否成功
        Boolean skxRefundCancelSyncYyediResult = Arguments.isNull(refundExtra.getSkxRefundCancelSyncYyediResult()) ? Boolean.FALSE : refundExtra.getSkxRefundCancelSyncYyediResult();
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        String syncErrorMsg = "";
        if (!skxRefundCancelSyncSkxResult) {
            try {
                String response = sycHkOrderCancelApi.doCancelOrder(shipmentExtra.getErpOrderShopCode(), refund.getRefundCode(), 0, 1);
                SycRefundResponse sycRefundResponse = JsonMapper.nonEmptyMapper().fromJson(response, SycRefundResponse.class);
                HkResponseHead head = sycRefundResponse.getHead();
                if (Objects.equals(head.getCode(), "0")) {
                    skxRefundCancelSyncSkxResult = Boolean.TRUE;
                    //更新标记位
                    refundExtra.setSkxRefundCancelSyncSkxResult(Boolean.TRUE);
                    Refund update = new Refund();
                    update.setId(refund.getId());
                    Map<String, String> extraMap = refund.getExtra();
                    extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    update.setExtra(extraMap);
                    Response<Boolean> updateRes = refundWriteLogic.update(update);
                    if (!updateRes.isSuccess()) {
                        log.error("update refund:{} fail,error:{}", update, updateRes.getError());
                    }
                } else {
                    skxRefundCancelSyncSkxResult = Boolean.FALSE;
                    //更新同步状态
                    updateRefundCancelSyncResult(refund, Boolean.FALSE, skxRefundCancelSyncYyediResult);
                    syncErrorMsg = "同步失败，SKX返回信息:" + head.getMessage();
                    log.error(syncErrorMsg);
                }
            } catch (Exception e) {
                log.error("sync hk refund failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
                skxRefundCancelSyncSkxResult = Boolean.FALSE;
                //更新同步状态
                updateRefundCancelSyncResult(refund, Boolean.FALSE, skxRefundCancelSyncYyediResult);
                syncErrorMsg = "同步SKX失败";

            }
        }

        if (!skxRefundCancelSyncYyediResult) {
            try {
                //要根据不同步的售后单类型来决定同步成功或失败的状态
                YYEdiCancelInfo yyEdiCancelInfo = new YYEdiCancelInfo();
                yyEdiCancelInfo.setBillNo(refund.getRefundCode());
                yyEdiCancelInfo.setReMark(refund.getBuyerNote());
                String response = sycYYEdiRefundCancelApi.doCancelOrder(Lists.newArrayList(yyEdiCancelInfo));
                YYEdiResponse yyEdiResponse = JsonMapper.nonEmptyMapper().fromJson(response, YYEdiResponse.class);

                if (Objects.equals(yyEdiResponse.getErrorCode(), TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS) || Objects.equals(yyEdiResponse.getErrorCode(), TradeConstants.YYEDI_RESPONSE_CANCELED)) {
                    skxRefundCancelSyncYyediResult = Boolean.TRUE;
                    //更新标记位
                    refundExtra.setSkxRefundCancelSyncYyediResult(Boolean.TRUE);
                    Refund update = new Refund();
                    update.setId(refund.getId());
                    Map<String, String> extraMap = refund.getExtra();
                    extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                    update.setExtra(extraMap);
                    Response<Boolean> updateRes = refundWriteLogic.update(update);
                    if (!updateRes.isSuccess()) {
                        log.error("update refund:{} fail,error:{}", update, updateRes.getError());
                    }
                } else {
                    skxRefundCancelSyncYyediResult = Boolean.FALSE;
                    //更新同步状态
                    updateRefundCancelSyncResult(refund, skxRefundCancelSyncSkxResult, Boolean.FALSE);
                    syncErrorMsg = "同步失败，yyedi返回信息:" + yyEdiResponse.getErrorCode();
                    log.error(syncErrorMsg);
                }
            } catch (Exception e) {
                log.error("sync yyedi refund fail failed,refundId is({}) cause by({})", refund.getId(), Throwables.getStackTraceAsString(e));
                //更新同步状态
                skxRefundCancelSyncYyediResult = Boolean.FALSE;
                updateRefundCancelSyncResult(refund, skxRefundCancelSyncSkxResult, Boolean.FALSE);
                syncErrorMsg = "同步YYEDI失败";
            }
        }

        if (skxRefundCancelSyncSkxResult && skxRefundCancelSyncYyediResult) {
            //同步调用成功后，更新售后单的状态
            Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            return Response.ok(Boolean.TRUE);
        } else {
            Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                return Response.fail(updateSyncStatusRes.getError());
            }
            return Response.fail(syncErrorMsg);
        }
    }

    /**
     * 组装售后单同步恒康参数1
     *
     * @param refund
     * @return
     */
    public SycHkRefund makeSyncHkRefund(Refund refund) {
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        SycHkRefund sycHkRefund = new SycHkRefund();
        //中台退换货单号
        sycHkRefund.setRefundNo(refund.getRefundCode());
        //中台原主订单号--发货单号
        sycHkRefund.setOrderNo(refundExtra.getShipmentId());
        //中台店铺id
        sycHkRefund.setShopId(String.valueOf(shipmentExtra.getErpOrderShopCode()));
        sycHkRefund.setShopName("斯凯奇");
        //退货仓
        if (refundExtra.getWarehouseId()!=null){
            Response<Warehouse> response = warehouseReadService.findById(refundExtra.getWarehouseId());
            if (!response.isSuccess()){
                log.error("find warehouse by id :{} failed,  cause:{}",shipmentExtra.getWarehouseId(),response.getError());
                throw new ServiceException(response.getError());
            }
            Warehouse warehouse = response.getResult();
            sycHkRefund.setStockId(warehouse.getInnerCode());
        }
        sycHkRefund.setPerformanceShopId(String.valueOf(shipmentExtra.getErpPerformanceShopCode()));
        //退款金额为页面上申请的退款金额
        sycHkRefund.setRefundOrderAmount(new BigDecimal(refund.getFee()==null?0:refund.getFee()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN).toString());
        sycHkRefund.setRefundFreight(0);
        //换货是在中台完成,不通知恒康,所以只有退款退货,仅退款两项
        //中台状态1:售后退款,2:退货退款,恒康状态0:退货退款,1:仅退款
        sycHkRefund.setType(String.valueOf(this.getHkRefundType(refund).value()));
        //默认状态为待仓库接收
        sycHkRefund.setStatus(String.valueOf(4));
        //售后单创建时间
        sycHkRefund.setCreatedDate(formatter.print(refund.getCreatedAt().getTime()));
        sycHkRefund.setTotalRefund(new BigDecimal(refund.getFee()==null?0:refund.getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
        //寄回物流单号
        sycHkRefund.setLogisticsCode(refundExtra.getShipmentSerialNo());
        //寄回物流公司代码
        sycHkRefund.setLogisticsCompany(refundExtra.getShipmentCorpCode());
        //订单来源
        sycHkRefund.setOnlineType(String.valueOf(syncShipmentLogic.getHkOnlinePay(shopOrder).getValue()));
        sycHkRefund.setMemo(refund.getBuyerNote());
        return sycHkRefund;
    }

    /**
     * 组装售后单同步恒康参数2
     *
     * @param refund
     * @return
     */
    public List<SycHkRefundItem> makeSycHkRefundItemList(Refund refund) {
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refund.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        List<SycHkRefundItem> items = Lists.newArrayList();
        for (RefundItem refundItem : refundItems) {
            SycHkRefundItem item = new SycHkRefundItem();
            //中台退换货子单号,用refunId与skuCode拼接
            item.setRefundSubNo(refund.getId() + "-" + refundItem.getSkuCode());
            //原销售来源子单号
            item.setOrderSubNo(refundExtra.getShipmentId() + "-" + refundItem.getSkuCode());
            //恒康商品条码
            item.setBarCode(refundItem.getSkuCode());
            //商品数量
            item.setItemNum(refundItem.getApplyQuantity());
            //换货原因,可不填
            item.setReason(refund.getBuyerNote());
            //商品价格
            item.setSalePrice(new BigDecimal(refundItem.getSkuPrice()==null?0:refundItem.getSkuPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //商品总净价
            item.setRefundAmount(new BigDecimal(refundItem.getFee()==null?0:refundItem.getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            //商品名称
            item.setItemName(refundItem.getSkuName());
            //外部订单号
            item.setOnlineOrderNo(shopOrder.getOutId());
            items.add(item);
        }

        return items;
    }

    /**
     * 组装取消恒康返回参数
     *
     * @param response
     * @return
     */
    public HkResponseHead makeHkResponseHead(String response) throws IOException {
        HkResponseHead responseHead = new HkResponseHead();

        Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);
        if (CollectionUtils.isEmpty(responnseMap)) {
            log.error("sync hk and refundResponseMap is null");
            throw new ServiceException("refund.responseMap.is.null");
        }
        if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
            log.error("refund hk shipmentResponseBody not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
            throw new JsonResponseException("refund.responseMap.head.is.null");
        }
        HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);

        return responseHead;
    }

    /**
     * 组装同步恒康售后单返回参数
     *
     * @param response
     * @return
     */
    public SycRefundResponse makeSycRefundResponse(String response) throws IOException {
        SycRefundResponse sycRefundResponse = new SycRefundResponse();

        Map<String, String> responnseMap = (Map) objectMapper.readValue(response, JacksonType.MAP_OF_STRING);

        if (CollectionUtils.isEmpty(responnseMap)) {
            log.error("sync cancel hk and refundResponseMap is null");
            throw new ServiceException("refund.responseMap.is.null");
        }
        if (!responnseMap.containsKey(TradeConstants.HK_RESPONSE_HEAD)) {
            log.error("refundResponseMap not contain key:{}", "", TradeConstants.HK_RESPONSE_HEAD);
            throw new ServiceException("refund.responseMap.head.is.null");
        }
        //获取响应头
        HkResponseHead head = mapper.fromJson(responnseMap.get(TradeConstants.HK_RESPONSE_HEAD), HkResponseHead.class);
        sycRefundResponse.setHead(head);
        //如果存在响应body,则返回响应body
        if (responnseMap.containsKey(TradeConstants.SYNC_HK_REFUND_BODY)) {
            SycHkRefundResponseBody refundBody = mapper.fromJson(responnseMap.get(TradeConstants.SYNC_HK_REFUND_BODY), SycHkRefundResponseBody.class);
            sycRefundResponse.setRefundBody(refundBody);
        }
        return sycRefundResponse;
    }

    /**
     * 将中台售后类型映射为恒康的售后类型
     *
     * @param refund
     * @return
     */
    private HkRefundType getHkRefundType(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        switch (middleRefundType) {
            case AFTER_SALES_REFUND:
                return HkRefundType.HK_AFTER_SALES_REFUND;
            case ON_SALES_REFUND:
                return HkRefundType.HK_AFTER_SALES_REFUND;
            case AFTER_SALES_RETURN:
                return HkRefundType.HK_AFTER_SALES_RETURN;
            case AFTER_SALES_CHANGE:
                return HkRefundType.HK_AFTER_SALES_RETURN;
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }
    }

    /**
     * 获取恒康
     * @param warehouseId
     * @return
     */
    private String getHkWarehouseCodeById(long warehouseId){
        Response<Warehouse> response = warehouseReadService.findById(warehouseId);
        if (!response.isSuccess()){
            log.error("find warehouse by id :{} failed",warehouseId);
            throw new ServiceException("find.warehouse.failed");
        }
        return response.getResult().getCode();
    }
}
