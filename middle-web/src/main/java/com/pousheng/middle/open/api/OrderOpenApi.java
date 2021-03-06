package com.pousheng.middle.open.api;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.*;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposApi;
import com.pousheng.middle.web.order.sync.mpos.SyncMposOrderLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 订单open api
 * Created by songrenfei on 2017/6/15
 */
@OpenBean
@Slf4j
public class OrderOpenApi {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private RefundWriteLogic refundWriteLogic;
    @RpcConsumer
    private RefundReadLogic refundReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @RpcConsumer
    private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
    @RpcConsumer
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;
    @Autowired
    private SyncMposOrderLogic syncMposOrderLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private ReceiveSkxResultLogic receiveSkxResultLogic;
    @Autowired
    private SyncMposApi syncMposApi;
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();


    /**
     * 第三方ERP同步发货单处理结果
     *
     * @param data 处理结果 约束大小为50个
     * @return 是否同步成功
     */
    @OpenMethod(key = "erp.shipment.handle.result", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncErpHandleResult(@NotNull(message = "handle.data.is.null") String data) {
        log.info("ERP-SYNC-SHIPMENT-HANDLE-RESULT-START param: data [{}] ", data);
        List<ErpHandleShipmentResult> results = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class, ErpHandleShipmentResult.class));
        try {
            for (ErpHandleShipmentResult result : results) {
                String shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                String erpShipmentId = result.getErpShipmentId();
                handleResult(shipmentId, handleResult, erpShipmentId);
            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("erp shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("erp shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }
        log.info("ERP-SYNC-SHIPMENT-HANDLE-RESULT-END param: data [{}]", data);
    }


    /**
     * 第三方ERP同步发货完成状态到中台
     *
     * @param shipmentId       中台发货单号
     * @param shipmentId       第三方ERP发货单号
     * @param shipmentCorpCode 物流公司代码
     * @param shipmentSerialNo 物流单号
     * @param shipmentDate     发货时间
     * @return 是否同步成功
     */
    @OpenMethod(key = "erp.shipments.api", paramNames = {"shipmentId", "erpShipmentId",
            "shipmentCorpCode", "shipmentSerialNo",
            "shipmentDate"}, httpMethods = RequestMethod.POST)
    public void syncErpShipmentStatus(@NotNull(message = "shipment.id.is.null") String shipmentId,
                                      @NotEmpty(message = "erp.shipment.id.is.null") String erpShipmentId,
                                      @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                      @NotEmpty(message = "shipment.serial.is.empty") String shipmentSerialNo,
                                      @NotEmpty(message = "shipment.date.empty") String shipmentDate) {
        if (log.isDebugEnabled()) {
            log.debug("ERP-SHIPMENTS-API-START param: shipmentId [{}] erpShipmentId [{}] shipmentCorpCode [{}] shipmentSerialNo [{}] shipmentDate [{}]",
                    shipmentId, erpShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);
        }

        try {
            DateTime dt = DateTime.parse(shipmentDate, DFT);
            Shipment shipment;
            try {
                shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
            } catch (Exception e) {
                log.error("find shipment failed,shipment id is {} ,caused by {}", shipmentId, Throwables.getStackTraceAsString(e));
                throw new ServiceException("shipment.id.not.matching");
            }
            //判断状态及获取接下来的状态
            Flow flow = flowPicker.pickShipments();
            OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
            if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                log.error("shipment(id={})'s status({}) not fit for ship",
                        shipment.getId(), shipment.getStatus());
                throw new ServiceException("shipment.current.status.not.allow.ship");
            }
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

            if (!Objects.equals(erpShipmentId, shipmentExtra.getOutShipmentId())) {
                log.error("hk shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", erpShipmentId, shipment.getId(), shipmentExtra.getOutShipmentId());
                throw new ServiceException("hk.shipment.id.not.matching");
            }

            SkxShipInfo skxShipInfo = new SkxShipInfo();
            skxShipInfo.setShipmentId(shipmentId);
            skxShipInfo.setErpShipmentId(erpShipmentId);
            skxShipInfo.setShipmentCorpCode(shipmentCorpCode);
            skxShipInfo.setShipmentSerialNo(shipmentSerialNo);
            skxShipInfo.setShipmentDate(shipmentDate);
            receiveSkxResultLogic.createShipmentResultTask(skxShipInfo);

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }
        //this.syncHkShipmentStatus(shipmentId,erpShipmentId,shipmentCorpCode,shipmentSerialNo,shipmentDate);
        if (log.isDebugEnabled()) {
            log.debug("ERP-SHIPMENTS-API-END param: shipmentId [{}] erpShipmentId [{}] shipmentCorpCode [{}] shipmentSerialNo [{}] shipmentDate [{}]",
                    shipmentId, erpShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);
        }
    }


    /**
     * 恒康同步发货单处理结果
     *
     * @param data 处理结果
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipment.handle.result", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncHkHandleResult(@NotNull(message = "handle.data.is.null") String data) {
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-START param: data [{}] ", data);
        List<HkHandleShipmentResult> results = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class, HkHandleShipmentResult.class));
        try {
            for (HkHandleShipmentResult result : results) {
                String shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                String hkShipmentId = result.getHkShipmentId();
                handleResult(shipmentId, handleResult, hkShipmentId);
            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-END param: data [{}] ", data);
    }

    private void handleResult(String shipmentId, Boolean handleResult, String erpShipmentId) {
        Shipment shipment = null;
        try {
            shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
        } catch (Exception e) {
            log.error("find shipment failed,shipment id is {} ,caused by {}", shipmentId, Throwables.getStackTraceAsString(e));
            return;
        }
        //冗余恒康发货单号
        //更新发货单的状态
        if (handleResult) {
            //如果发货单已受理，则跳过
            if (shipment.getStatus() >= MiddleShipmentsStatus.WAIT_SHIP.getValue()) {
                log.warn("shipment(id:{}) duplicate request to handle,so skip", shipment.getId());
                return;
            }
            Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentIdAndCurrentStatus(shipment.getId(), shipment.getStatus(), MiddleShipmentsStatus.WAIT_SHIP.getValue());
            if (!updateRes.isSuccess()) {
                log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(), MiddleShipmentsStatus.WAIT_SHIP.getValue(), updateRes.getError());
            }
            //更新恒康shipmentId
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            shipmentExtra.setOutShipmentId(erpShipmentId);
            Map<String, String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shipmentExtra));
            update.setExtra(extraMap);
            log.info("start update erpShipmentId is {}", erpShipmentId);
            shipmentWiteLogic.update(update);
            log.info("end update erpShipmentId is {}", erpShipmentId);
        } else {
            OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
            }
        }
    }

    /**
     * 恒康同步发货完成状态到中台
     *
     * @param shipmentId       中台发货单号
     * @param hkShipmentId     恒康发货单号
     * @param shipmentCorpCode 物流公司代码
     * @param shipmentSerialNo 物流单号
     * @param shipmentDate     发货时间
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipments.api", paramNames = {"shipmentId", "hkShipmentId",
            "shipmentCorpCode", "shipmentSerialNo",
            "shipmentDate"}, httpMethods = RequestMethod.POST)
    public void syncHkShipmentStatus(@NotNull(message = "shipment.id.is.null") String shipmentId,
                                     @NotEmpty(message = "hk.shipment.id.is.null") String hkShipmentId,
                                     @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                     @NotEmpty(message = "shipment.serial.is.empty") String shipmentSerialNo,
                                     @NotEmpty(message = "shipment.date.empty") String shipmentDate
    ) {
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}", shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            Shipment shipment = null;
            try {
                shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
            } catch (Exception e) {
                log.error("find shipment failed,shipment id is {} ,caused by {}", shipmentId, Throwables.getStackTraceAsString(e));
                return;
            }
            //判断状态及获取接下来的状态
            Flow flow = flowPicker.pickShipments();
            OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
            if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                log.error("shipment(id={})'s status({}) not fit for ship",
                        shipment.getId(), shipment.getStatus());
                throw new ServiceException("shipment.current.status.not.allow.ship");
            }
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(shipment);
            //店发默认全部发货
            for (ShipmentItem s : items) {
                s.setShipQuantity(s.getQuantity());
            }
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

            if (!Objects.equals(hkShipmentId, shipmentExtra.getOutShipmentId())) {
                log.error("hk shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", hkShipmentId, shipment.getId(), shipmentExtra.getOutShipmentId());
                throw new ServiceException("hk.shipment.id.not.matching");
            }


            //封装更新信息
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            Map<String, String> extraMap = shipment.getExtra();
            shipmentExtra.setShipmentSerialNo(shipmentSerialNo);
            shipmentExtra.setShipmentCorpCode(shipmentCorpCode);
            //通过恒康代码查找快递名称
            ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(shipmentCorpCode);
            shipmentExtra.setShipmentCorpName(expressCode.getName());
            shipmentExtra.setShipmentDate(dt.toDate());
            extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, mapper.toJson(items));
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
            update.setExtra(extraMap);
            update.setShipmentSerialNo(shipmentSerialNo);
            update.setShipmentCorpCode(shipmentCorpCode);

            //更新状态
            Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentIdAndCurrentStatus(shipment.getId(), shipment.getStatus(), targetStatus);
            if (!updateStatusRes.isSuccess()) {
                log.error("update shipment(id:{}) status to :{} fail,error:{}", shipment.getId(), targetStatus, updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新基本信息
            Response<Boolean> updateRes = shipmentWriteService.update(update);
            if (!updateRes.isSuccess()) {
                log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
                throw new ServiceException(updateRes.getError());
            }
            //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
            hkShipmentDoneLogic.doneShipment(shipment);

            //同步pos单到恒康
            //生成发货单同步恒康生成pos的任务
            PoushengCompensateBiz biz = new PoushengCompensateBiz();
            biz.setBizId(String.valueOf(shipment.getId()));
            biz.setBizType(PoushengCompensateBizType.SYNC_ORDER_POS_TO_HK.name());
            biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
            compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}", shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);

    }

    /**
     * skx将售后单售后结果通知给中台
     *
     * @param refundOrderId
     * @param erpRefundOrderId
     * @param itemInfo
     * @param receivedDate
     */
    @OpenMethod(key = "erp.refund.confirm.received.api", paramNames = {"refundOrderId", "erpRefundOrderId", "itemInfo",
            "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncErpRefundStatus(String refundOrderId,
                                    @NotEmpty(message = "hk.refund.order.id.is.null") String erpRefundOrderId,
                                    @NotEmpty(message = "item.info.empty") String itemInfo,
                                    @NotEmpty(message = "received.date.empty") String receivedDate
    ) {
        if (log.isDebugEnabled()) {
            log.debug("ERP-REFUND-CONFIRM-RECEIVED-API-START param: refundOrderId [{}] erpRefundOrderId [{}] itemInfo [{}] receivedDate [{}]",
                    refundOrderId, erpRefundOrderId, itemInfo, receivedDate);
        }
        syncHkRefundStatus(refundOrderId, erpRefundOrderId, itemInfo, receivedDate);
        if (log.isDebugEnabled()) {
            log.debug("ERP-REFUND-CONFIRM-RECEIVED-API-END param: refundOrderId [{}] erpRefundOrderId [{}] itemInfo [{}] receivedDate [{}]",
                    refundOrderId, erpRefundOrderId, itemInfo, receivedDate);
        }
    }


    /**
     * skx取消中台售后发货单
     *
     * @param shipmentCode
     * @param refundCode
     */
    @OpenMethod(key = "hk.cancel.refund.shipment", paramNames = {"shipmentCode", "refundCode"}, httpMethods = RequestMethod.POST)
    public void cancelAfterSaleSkxShipments(String shipmentCode, String refundCode) {
        if (log.isDebugEnabled()) {
            log.debug("HK-CANCEL-REFUND-SHIPMENT-START param: shipmentId [{}] refundId [{}]", shipmentCode, refundCode);
        }
        Refund refund = refundReadLogic.findRefundByRefundCode(refundCode);
        //占库发货单整体取消
        refundWriteLogic.cancelSkxAfterSaleOccupyShipments(refund.getId());
        //此时判断换货单的状态
        if (Objects.equals(refund.getStatus(), MiddleRefundStatus.RETURN_DONE_WAIT_CONFIRM_OCCUPY_SHIPMENT.getValue())) {
            //状态节点为【退货完成待确认发货】将售后单状态回滚到同步成功待创建发货单
            log.info("hk-cancel-refund-shipment,refundCode {},currentStatus{}", refundCode, refund.getStatus());
            refundWriteLogic.updateStatus(refund, MiddleOrderEvent.AFTER_SALE_CHANGE_RE_CREATE_SHIPMENT.toOrderOperation());

        } else if (Objects.equals(refund.getStatus(), MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue())) {
            //状态节点为【退货完成待生成发货单】
            log.info("hk-cancel-refund-shipment,refundCode {},currentStatus{}", refundCode, refund.getStatus());
            return;
        } else {
            //状态节点为【待退货完成】
            log.info("hk-cancel-refund-shipment,refundCode {},currentStatus{}", refundCode, refund.getStatus());
        }
        //售后单已经申请售后的数量设置为0
        List<RefundItem> exchangeItems = refundReadLogic.findRefundChangeItems(refund);
        List<RefundItem> newExchangeItems = com.google.common.collect.Lists.newArrayList();
        for (RefundItem refundItem : exchangeItems) {
            refundItem.setAlreadyHandleNumber(0);
            newExchangeItems.add(refundItem);
        }
        Refund newRefund = refundReadLogic.findRefundById(refund.getId());
        Map<String, String> refundExtra = newRefund.getExtra();
        refundExtra.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(newExchangeItems));
        newRefund.setExtra(refundExtra);
        refundWriteLogic.update(newRefund);

        if (log.isDebugEnabled()) {
            log.debug("HK-CANCEL-REFUND-SHIPMENT-END param: shipmentId [{}] refundId [{}]", shipmentCode, refundCode);
        }

    }


    /**
     * 恒康将售后单售后结果通知给中台
     *
     * @param refundOrderId
     * @param hkRefundOrderId
     * @param itemInfo
     * @param receivedDate
     */
    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId", "hkRefundOrderId", "itemInfo",
            "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(String refundOrderId,
                                   @NotEmpty(message = "hk.refund.order.id.is.null") String hkRefundOrderId,
                                   @NotEmpty(message = "item.info.empty") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate
    ) {
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, hkRefundOrderId, itemInfo, receivedDate);
        try {
            if (refundOrderId == null) {
                return;
            }
            Refund refund = refundReadLogic.findRefundByRefundCode(refundOrderId);
            Map<String, String> extraMap = refund.getExtra();
            String hkRefundId = extraMap.get(TradeConstants.HK_REFUND_ID);
            //仅退款是没有hkRefundOrderId,所以放开这边的校验
           /* if (!Objects.equals(hkRefundOrderId, hkRefundId)) {
                log.error("hk refund id:{} not equal middle refund(id:{} ) out id:{}", hkRefundOrderId, refund.getId(), hkRefundId);
                throw new ServiceException("hk.refund.id.not.matching");
            }*/


            DateTime dt = DateTime.parse(receivedDate, DFT);
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            refundExtra.setIslock(1);
            List<HkConfirmReturnItemInfo> hkConfirmReturnItemInfos = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, HkConfirmReturnItemInfo.class));

            refundExtra.setHkConfirmItemInfos(hkConfirmReturnItemInfos);

            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                throw new OPServerException(200, "已经通知中台退货，请勿再次通知");
            }

            //更新扩展信息
            Refund update = new Refund();
            update.setId(refund.getId());
            Map<String, String> extra = refund.getExtra();
            extra.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            update.setExtra(extra);

            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update rMatrixRequestHeadefund(id:{}) extra:{} fail,error:{}", refundOrderId, refundExtra, updateExtraRes.getError());
                //这就就不抛出错了，中台自己处理即可。
            }
            // 通知mpos收到退货
            if (refund.getShopName().startsWith("mpos")) {
                syncMposOrderLogic.notifyMposRefundReceived(refund.getOutId());
            }

            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
            //仓发拒收单直接同步订单恒康就好了
            if (Objects.equals(shipment.getShipWay(), 2) && Objects.equals(refund.getRefundType(), MiddleRefundType.REJECT_GOODS.value())) {
                Response<Boolean> r = syncRefundPosLogic.syncSaleRefuseToHK(refund);
                if (!r.isSuccess()) {
                    Map<String, Object> param1 = Maps.newHashMap();
                    param1.put("refundId", refund.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_SALE_REFUSE_TO_HK, r.getError());
                }
            } else {
                //售后单的pos任务
                PoushengCompensateBiz biz = new PoushengCompensateBiz();
                biz.setBizId(String.valueOf(refund.getId()));
                biz.setBizType(PoushengCompensateBizType.SYNC_AFTERSALE_POS_TO_HK.name());
                biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
                compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);

            }
            //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
            refundWriteLogic.getThirdRefundResult(refund);

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync refund confirm to middle fail,error:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync refund confirm to middle fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }
        log.info("HK-SYNC-REFUND-STATUS-END param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, hkRefundOrderId, itemInfo, receivedDate);
    }


    /**
     * 恒康同步pos信息到中台
     *
     * @param orderId      订单id
     * @param orderType    订单类型1.销售发货单,2.售后订单
     * @param posSerialNo  pos单号
     * @param posType      pos单类型
     * @param posAmt       pos金额
     * @param posCreatedAt pos单创建时间
     */
    @OpenMethod(key = "hk.pos.api", paramNames = {"orderId", "orderType", "posSerialNo", "posType",
            "posAmt", "posCreatedAt"}, httpMethods = RequestMethod.POST)
    public void syncHkPosStatus(@NotNull(message = "order.id.is.null") String orderId,
                                @NotEmpty(message = "hk.order.type.is.null") String orderType,
                                @NotEmpty(message = "pos.serial.is.empty") String posSerialNo,
                                @NotNull(message = "pos.type.is.null") Integer posType,
                                @NotNull(message = "pos.amt.is.empty") String posAmt,
                                @NotEmpty(message = "pos.created.time.is.empty") String posCreatedAt) {
        log.info("HK-SYNC-POS-INFO-START param orderId is:{} orderType is:{}  posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{}", orderId, orderType, posSerialNo, posType, posAmt, posCreatedAt);

        try {

            DateTime dPos = DateTime.parse(posCreatedAt, DFT);
            PoushengSettlementPos pos = new PoushengSettlementPos();
            if (Objects.equals(orderType, "1")) { //pos单类型是1有两种订单类型，第一种是正常的销售发货,一种是换货生成的发货单
                OrderShipment orderShipment = null;
                try {
                    orderShipment = shipmentReadLogic.findOrderShipmentByShipmentCode(orderId);

                } catch (Exception e) {
                    log.error("find order shipment failed,shipment id is {} ,caused by {}", orderId, Throwables.getStackTraceAsString(e));
                    return;
                }
                if (Objects.equals(orderShipment.getType(), 1)) {
                    pos.setOrderId(orderShipment.getOrderCode());
                    pos.setShipType(1);
                } else {
                    pos.setOrderId(orderShipment.getAfterSaleOrderCode());
                    pos.setShipType(2);
                    pos.setPosDoneAt(new Date());
                }
                String amt = String.valueOf(new BigDecimal(Double.valueOf(posAmt) * 100).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(amt));
                pos.setShipmentId(orderId);
                pos.setPosType(Integer.valueOf(posType));
                pos.setPosSerialNo(posSerialNo);
                pos.setShopId(orderShipment.getShopId());
                pos.setShopName(orderShipment.getShopName());
                pos.setPosCreatedAt(dPos.toDate());


            } else if (Objects.equals(orderType, "2")) {
                Refund refund = null;
                try {
                    refund = refundReadLogic.findRefundByRefundCode(orderId);
                } catch (Exception e) {
                    log.error("find refund failed,refund id is {} ,caused by {}", orderId, Throwables.getStackTraceAsString(e));
                    return;
                }
                pos.setOrderId(refund.getRefundCode());
                String amt = String.valueOf(new BigDecimal(Double.valueOf(posAmt) * 100).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(amt));
                pos.setPosType(Integer.valueOf(posType));
                pos.setShipType(3);
                pos.setPosSerialNo(posSerialNo);
                pos.setShopId(refund.getShopId());
                pos.setShopName(refund.getShopName());
                pos.setPosCreatedAt(dPos.toDate());
                pos.setPosDoneAt(new Date());
            } else {
                throw new ServiceException("invalid.order.type");
            }
            Response<PoushengSettlementPos> rP = poushengSettlementPosReadService.findByPosSerialNo(posSerialNo);
            if (!rP.isSuccess()) {
                log.error("find pousheng settlement pos failed, posSerialNo is {},caused by {}", posSerialNo, rP.getError());
                return;
            }
            if (!Objects.isNull(rP.getResult())) {
                log.error("duplicate posSerialNo is {},caused by {}", posSerialNo, rP.getError());
                return;
            }
            Response<Long> r = poushengSettlementPosWriteService.create(pos);
            if (!r.isSuccess()) {
                log.error("create poushengSettlementPos failed, poushengSettlementPos:{},caused by ", pos, r.getError());
                throw new ServiceException(r.getError());
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync posInfo(id:{}) to pousheng fail,error:{}", orderId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync posInfo(id:{}) fail,orderType is ({})cause:{}", orderId, orderType, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }

        log.info("HK-SYNC-POS-INFO-END param orderId is:{} orderType is:{}  posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{}", orderId, orderType, posSerialNo, posType, posAmt, posCreatedAt);
    }


    /**
     * 取消订单
     *
     * @param data 请求内容
     * @return 订单状态
     */
    @OpenMethod(key = "out.order.cancel.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public String syncOrderCancel(@NotNull(message = "cancel.data.is.null") String data) {
        log.info("SYNC-OUT-ORDER-CANCEL-START DATA is:{} ", data);
        CancelOutOrderInfo cancelOutOrderInfo = JSON_MAPPER.fromJson(data, CancelOutOrderInfo.class);
        String outId = cancelOutOrderInfo.getOutOrderId();
        String outFrom = cancelOutOrderInfo.getChannel();
        Long orderId = findOrderIdByOutIdAndOutFrom(outId, outFrom).getId();
        String result = TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS;
        try {
            if (MiddleChannel.YUNJURT.getValue().equals(outFrom)) {
                orderWriteLogic.autoCancelJitRealtimeOrder(orderId);
            } else {
                orderWriteLogic.autoCancelShopOrder(orderId);
            }
        } catch (JsonResponseException e) {
            log.error("cancel shop order id:{} fail", orderId);
            result = TradeConstants.YYEDI_RESPONSE_CODE_FAILED;
            if (!MiddleChannel.YUNJURT.getValue().equals(outFrom) && "sync.cancel.order.request.to.yyedi.or.mpos.fail".equals(e.getMessage())) {
                List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(orderId);
                Shipment shipment = shipments.stream().filter(Objects::nonNull).filter(it -> (!Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))).findFirst().get();
                if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue())) {
                    result = TradeConstants.YYEDI_RESPONSE_CODE_ING;
                }
            }
        }
        log.info("SYNC-OUT-ORDER-CANCEL-END DATA: {}", data);
        return result;
    }


    /**
     * 查询订单状态
     *
     * @param data 请求内容
     * @return 订单状态
     */
    @OpenMethod(key = "out.order.status.search.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public String searchOrderStatus(@NotNull(message = "cancel.data.is.null") String data) {
        log.info("SYNC-OUT-ORDER-SEARCH-START DATA is:{} ", data);
        CancelOutOrderInfo cancelOutOrderInfo = JSON_MAPPER.fromJson(data, CancelOutOrderInfo.class);
        String outId = cancelOutOrderInfo.getOutOrderId();
        String outFrom = cancelOutOrderInfo.getChannel();
        ShopOrder shopOrder = findOrderIdByOutIdAndOutFrom(outId, outFrom);
        String result = null;
        if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CANCEL.getValue())) {
            result = TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS;
        } else {
            List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrder.getId());
            Shipment shipment = shipments.stream().filter(Objects::nonNull).filter(it -> (!Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(), MiddleShipmentsStatus.REJECTED.getValue()))).findFirst().get();
            if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue())) {
                result = TradeConstants.YYEDI_RESPONSE_CODE_ING;
            } else {
                result = TradeConstants.YYEDI_RESPONSE_CODE_FAILED;
            }
        }
        log.info("SYNC-OUT-ORDER-SEARCH-END DATA:", data);
        return result;
    }


    private ShopOrder findOrderIdByOutIdAndOutFrom(String outId, String outFrom) {


        //恒康预收单的取消，需要特殊处理，要先去电商库存中查询电商的订单，然后再查询中台的订单（outId在电商库中有）
        if (Objects.equals(outFrom, "hk")) {
            //查询电商
            MposResponse resp = mapper.fromJson(syncMposApi.queryEcpOrderIdByOutIdForHk(outId), MposResponse.class);
            if (!resp.isSuccess()) {
                log.error("query mpos order by out id:{} fail,error:{}", outId, resp.getError());
                throw new OPServerException(200, resp.getError());
            }
            //将outId替换为电商的订单id
            outId = resp.getResult();
            outFrom = "official";
        }

        Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(outId, outFrom);
        if (!findShopOrder.isSuccess()) {
            log.error("fail to find shop order by outId={},outFrom={} when sync receiver info,cause:{}",
                    outId, outFrom, findShopOrder.getError());
            throw new OPServerException(200, findShopOrder.getError());
        }
        Optional<ShopOrder> shopOrderOptional = findShopOrder.getResult();
        if (!shopOrderOptional.isPresent()) {
            log.error("shop order not found where outId={},outFrom=:{} when sync receiver info", outId, outFrom);
            throw new OPServerException(200, "order.not.found");
        }
        return shopOrderOptional.get();

    }


    /**
     * 同步售后单
     *
     * @param data 售后单信息
     * @return 是否同步成功
     */
    @OpenMethod(key = "out.order.refund.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void createRefundOrder(@NotNull(message = "refund.data.is.null") String data) {
        log.info("SYNC-OUT-ORDER-REFUND-START DATA is:{} ", data);

        log.info("out.order.refund.api req={}", data);
        OutOrderApplyRefund applyRefund = JSON_MAPPER.fromJson(data, OutOrderApplyRefund.class);
        if (Objects.isNull(applyRefund)) {
            throw new OPServerException(200, "parameter deserialize failed");
        }
        OutRefundOrder refundOrder = applyRefund.getRefund();
        if (Objects.isNull(refundOrder)) {
            throw new OPServerException(200, "parameter rufund can't be empty");
        }
        if (CollectionUtils.isEmpty(applyRefund.getItems())) {
            throw new OPServerException(200, "items can't be empty");
        }
        String outId = refundOrder.getOutOrderId();
        String outFrom = refundOrder.getChannel();
        Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(outId, outFrom);
        if (!findShopOrder.isSuccess() || !findShopOrder.getResult().isPresent()) {
            log.error("fail to find shop order by outId={},outFrom={} when sync receiver info,cause:{}",
                    outId, outFrom, findShopOrder.getError());
            throw new OPServerException(200, findShopOrder.getError());
        }
        ShopOrder shopOrder = findShopOrder.getResult().get();

        SubmitRefundInfo refundInfo = new SubmitRefundInfo();

        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess() || CollectionUtils.isEmpty(response.getResult())) {
            log.error("fail to find OrderShipmentv  by orderId={},OrderLevel={} when sync receiver info,cause:{}",
                    shopOrder.getId(), OrderLevel.SHOP, response.getError());
            throw new OPServerException(200, findShopOrder.getError());
        }
        refundInfo.setOrderId(shopOrder.getId());
        refundInfo.setOrderCode(shopOrder.getOrderCode());
        OrderShipment shipment = response.getResult().get(0);
        refundInfo.setOutAfterSaleOrderId(refundOrder.getOutAfterSaleOrderId()); //售后单id
        refundInfo.setShipmentId(shipment.getShipmentId());
        refundInfo.setShipmentCode(shipment.getShipmentCode());
        refundInfo.setRefundType(refundOrder.getType()); //退售后 类型
        refundInfo.setFee(refundOrder.getFee()); //金额
        ArrayList<EditSubmitRefundItem> refundItems = Lists.newArrayList();
        applyRefund.getItems().forEach(x -> {
            EditSubmitRefundItem refundItem = new EditSubmitRefundItem();
            refundItem.setRefundSkuCode(x.getSkuCode());
            refundItem.setRefundQuantity(x.getQuantity());
            refundItem.setFee(x.getFee());
            refundItem.setItemName(x.getItemName()); //商品名称
            refundItem.setSkuAfterSaleId(x.getSkuAfterSaleId());//外部售后子单存放
            refundItems.add(refundItem);
        });
        refundInfo.setEditSubmitRefundItems(refundItems);//退 商品item
        refundInfo.setBuyerNote(refundOrder.getBuyerNote()); //买家备注
        // 自动推送
        refundInfo.setOperationType(2); //操作类型 //这里不需要同步给yyedi,需要客服审核手动触发推送给yyedi
        refundInfo.setReturnStockid(refundOrder.getReturnStockid()); //退货仓id 文案有点问题存到warehousId
        // 快递单号
        refundInfo.setShipmentSerialNo(refundOrder.getExpressCode());
        refundWriteLogic.createYunJURefund(refundInfo);

        log.info("cancelOutOrderInfo:", applyRefund);
    }

    /**
     * 取消售后单
     *
     * @param data 售后单信息
     * @return 是否同步成功
     */
    @OpenMethod(key = "out.refund.cancel.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public String cancelRefundOrder(@NotNull(message = "cancel.refund.data.is.null") String data) {
        log.info("SYNC-OUT-REFUND-CANCEL-START DATA is:{} ", data);

        try {
            CancelOutRefundInfo cancelOutRefundInfo = JSON_MAPPER.fromJson(data, CancelOutRefundInfo.class);
            String outerRefundId = cancelOutRefundInfo.getOutAfterSaleOrderId();
            Refund refund;
            try {
                refund = refundReadLogic.findRefundByOutId(MiddleChannel.YUNJUBBC.getValue() + "_" + outerRefundId);
                //refund.setRefundType(MiddleRefundType.AFTER_SALES_RETURN.value());//退货退款
            } catch (JsonResponseException e) {
                log.error("find refund by out id:{} fail,error:{}", outerRefundId, e.getMessage());
                throw new OPServerException(200, e.getMessage());
            }

            DateTime dt = DateTime.parse(cancelOutRefundInfo.getApplyAt(), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
            OpenClientAfterSale afterSale = new OpenClientAfterSale();
            afterSale.setStatus(OpenClientAfterSaleStatus.RETURN_CLOSED);
            afterSale.setReason(cancelOutRefundInfo.getBuyerNote());
            afterSale.setApplyAt(dt.toDate());

            Response<Boolean> response = refundWriteLogic.cancelRefund(refund, afterSale);
            if (!response.isSuccess()) {
                log.error("cancel refund(id:{}) fail,error:{}", refund.getId(), response.getError());
                refund = refundReadLogic.findRefundById(refund.getId());
                if (!CollectionUtils.isEmpty(refund.getExtra()) && refund.getExtra().containsKey(TradeConstants.WAIT_CANCEL_RESULT)
                        && refund.getExtra().get(TradeConstants.WAIT_CANCEL_RESULT).equals("1")) {
                    return TradeConstants.YYEDI_RESPONSE_CODE_ING;
                } else {
                    return TradeConstants.YYEDI_RESPONSE_CODE_FAILED;
                }
            }
            return TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS;
        } catch (Exception e) {
            log.error("SYNC-OUT-REFUND-CANCEL-START failed,cause:", e);
            return TradeConstants.YYEDI_RESPONSE_CODE_FAILED;
        }

    }


    //获取同步成功事件
    private OrderOperation getSyncConfirmSuccessOperation(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        if (Arguments.isNull(middleRefundType)) {
            log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
            throw new JsonResponseException("refund.type.invalid");
        }

        switch (middleRefundType) {
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            case AFTER_SALES_REFUND:
                log.error("refund(id:{}) type:{} not allow hk confirm", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.not.allow.hk.confirm");
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            case REJECT_GOODS:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }
}
