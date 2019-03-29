package com.pousheng.middle.open.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.dto.YJSyncCancelRequest;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiResponse;
import com.pousheng.middle.open.api.dto.YyEdiResponseDetail;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单派发中心回调中台接口
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/2
 * pousheng-middle
 */
@OpenBean
@Slf4j
public class yyEDIOpenApi {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private ReceiveYyediResultLogic receiveYyediResultLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private SyncYYEdiShipmentLogic syncYYEdiShipmentLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private ShipmentWriteManger shipmentWriteManger;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final String REFUND_SIGN = "ASS";

    /**
     * yyEDI回传发货信息
     *
     * @param shipInfo
     */
    @OpenMethod(key = "yyEDI.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentResult(String shipInfo) {
        log.info("YYEDI-SHIPMENT-INFO-START param: shipInfo [{}]", shipInfo);
        dealErpShipmentInfo(shipInfo);
        log.info("YYEDI-SHIPMENT-INFO-END param: shipInfo [{}]", shipInfo);
    }


    @OpenMethod(key = "yyEDI.shipments.cancel.result.api", paramNames = {"shipmentCode", "errorCode", "description"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentCancelResult(@NotEmpty(message = "shipment.code.is.null") String shipmentCode,
                                                 @NotEmpty(message = "error.code.is.null") String errorCode,
                                                 String description) {
        log.info("YYEDI-SHIPMENT-CANCEL-RESUKT-START param: shipmentCode {}, errorCode {}, description {} ", shipmentCode, errorCode, description);
        try {
            //如果是售后单
            if (shipmentCode.startsWith(REFUND_SIGN)) {
                Refund refund = refundReadLogic.findRefundByRefundCode(shipmentCode);
                //如果不是取消中 忽略请求
                if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.SYNC_HK_CANCEL_ING.getValue())) {
                    log.info("refund {} current status is {}", shipmentCode, refund.getStatus());
                    return;
                }
                OrderOperation syncSuccessOrderOperation;
                if (Objects.equals(errorCode, TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)) {
                    syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                    refundWriteLogic.rollbackRefundQuantities(refund);
                } else {
                    syncSuccessOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
                }
                refundWriteLogic.updateStatusLocking(refund, syncSuccessOrderOperation);

                //取消成功的情况下 售后换后要解锁库存
                if (Objects.equals(syncSuccessOrderOperation, MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation()) && Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
                    refundWriteLogic.cancelAfterSaleOccupyShipments(refund.getId());
                }
                log.info("YYEDI-SHIPMENT-CANCEL-RESUKT-END param: shipmentCode {}, errorCode {}, description {} ", shipmentCode, errorCode, description);
                return;
            }
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentCode(shipmentCode);
            ShopOrder order = orderReadLogic.findShopOrderById(orderShipment.getOrderId());
            //如果发货  单不是取消中 直接通知就行了
            if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue())) {
                if (Objects.equals(errorCode, TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)) {
                    //判断发货单类型，如果发货单类型是销售发货单正常处理
                    if (Objects.equals(shipment.getType(), ShipmentType.SALES_SHIP.value())) {
                        cancelYYediShipment(shipment);
                        List<Long> skuOrderIds = Lists.newArrayList(shipment.getSkuInfos().keySet());
                        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
                        //当发货单上带有订单取消标记时 取消发货单成功以后直接取消 订单级别记录的订单号 子单级别记录子单id
                        if (shipment.getExtra().containsKey(TradeConstants.SHIPMENT_CANCEL_BY_ORDER)) {
                            //当标记位记录的不是订单号时  则仅取消记录的子单id 剩余子单仅取消发货单 否则直接取消订单
                            List<SkuOrder> canceList = skuOrders.stream().filter(e -> e.getExtra().containsKey(TradeConstants.SHIPMENT_CANCEL_BY_ORDER)).collect(Collectors.toList());
                            List<SkuOrder> skuOrdersFilter = skuOrders.stream().filter(e -> !e.getExtra().containsKey(TradeConstants.SHIPMENT_CANCEL_BY_ORDER)).collect(Collectors.toList());
                            middleOrderWriteService.updateOrderStatusAndSkuQuantitiesForSku(order, skuOrdersFilter, canceList,
                                    MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation(), MiddleOrderEvent.REVOKE.toOrderOperation(), "");
                        } else {
                            middleOrderWriteService.updateOrderStatusAndSkuQuantities(order, skuOrders, MiddleOrderEvent.REVOKE.toOrderOperation());
                        }
                        //取消时若是占库发货单，则订单上面的占库标识要被取消
                        if (Objects.equals(shipment.getIsOccupyShipment(), ShipmentOccupyType.SALE_Y.toString())) {
                            shipmentWiteLogic.updateShipmentNote(order, OrderWaitHandleType.HANDLE_DONE.value());
                        }
                    }
                    //换货
                    if (Objects.equals(orderShipment.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
                        Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
                        if (Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_CONFIRM_RECEIVE.getValue())) {
                            throw new JsonResponseException("can.not.cancel.exchange.shipment");
                        }
                        cancelYYediShipment(shipment);
                        shipmentWiteLogic.rollbackChangeRefund(shipment, orderShipment, refund);

                        Map<String, String> extraMap = refund.getExtra();
                        log.info("=============EXCHANGE_REFUND="+extraMap.get(TradeConstants.EXCHANGE_REFUND));
                        if (extraMap.containsKey(TradeConstants.EXCHANGE_REFUND)&&"Y".equals(extraMap.get(TradeConstants.EXCHANGE_REFUND))) {
                            //换转退
                            refundWriteLogic.exchangeToRefund(refund.getId());
                        }
                    }
                    //丢件补发
                    if (Objects.equals(orderShipment.getType(), 3)) {
                        Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
                        if (Objects.equals(refund.getStatus(), MiddleRefundStatus.LOST_SHIPPED.getValue())) {
                            throw new JsonResponseException("can.not.cancel.lost.shipment");
                        }
                        cancelYYediShipment(shipment);
                        shipmentWiteLogic.rollbackLostRefund(shipment, orderShipment, refund);
                    }
                } else {
                    //更新状态取消失败
                    syncYYEdiShipmentLogic.updateShipmetSyncCancelFail(shipment);
                    log.error("订单派发中心返回信息:" + description);
                }
            }
            //如果是云聚bbc的发货单 且是因为取消订单而取消的发货单，需要通知云聚，抛出biz事件
            if (Objects.equals(order.getOutFrom(), MiddleChannel.YUNJUBBC.getValue()) && shipment.getExtra().containsKey(TradeConstants.SHIPMENT_CANCEL_BY_ORDER)) {
                YJSyncCancelRequest yyEdiResponse = new YJSyncCancelRequest();
                order = orderReadLogic.findShopOrderById(orderShipment.getOrderId());

                //云聚 1 成功 2失败
                yyEdiResponse.error_code(order.getStatus().equals(MiddleOrderStatus.CANCEL.getValue()) ? 1 : 2);
                yyEdiResponse.error_info(description);
                yyEdiResponse.order_sn(order.getOutId());
                PoushengCompensateBiz biz = new PoushengCompensateBiz();
                biz.setContext(mapper.toJson(yyEdiResponse));
                biz.setBizType(PoushengCompensateBizType.SYNC_CANCEL_TO_YJ.name());
                biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
                compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
            }

        } catch (JsonResponseException e) {
            log.error("yyedi shipment cancel result to pousheng fail,error:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("yyedi shipment cancel result failed，caused by {}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, e.getMessage());
        }
        log.info("YYEDI-SHIPMENT-CANCEL-RESUKT-END param: shipmentCode {}, errorCode {}, description {} ", shipmentCode, errorCode, description);

    }


    private void cancelYYediShipment(Shipment shipment) {
        OrderOperation operation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
        Response<Boolean> updateStatus = shipmentWiteLogic.updateStatusLocking(shipment, operation);
        if (!updateStatus.isSuccess()) {
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
            throw new JsonResponseException(updateStatus.getError());
        }
        if (Objects.equals(shipment.getType(), ShipmentType.SALES_SHIP.value())) {
            //回滚数量
            shipmentWriteManger.rollbackSkuOrderWaitHandleNumber(shipment);
        }
        //解锁库存
        mposSkuStockLogic.unLockStock(shipment);
        log.info("try to auto cancel shipment,shipment id is {} success", shipment.getId());
    }


    /**
     * yjERP回传发货信息
     *
     * @param shipInfo
     */
    @OpenMethod(key = "yj.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYJERPShipmentResult(String shipInfo) {
        log.info("YJERP-SHIPMENT-INFO-START param: shipInfo [{}]", shipInfo);
        dealErpShipmentInfo(shipInfo);
        log.info("YJERP-SHIPMENT-INFO-END param: shipInfo [{}]", shipInfo);
    }


    /**
     * wsm回传jit发货信息
     *
     * @param shipInfo
     */
    @OpenMethod(key = "jit.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveJitShipmentResult(String shipInfo) {
        log.info("WMS-JIT-SHIPMENT-INFO-START param: shipInfo [{}]", shipInfo);
        dealWmsShipmentInfo(shipInfo);
        log.info("WMS-JIT-SHIPMENT-INFO-END param: shipInfo [{}]", shipInfo);
    }


    /**
     * @param shipInfo yyEdi or yjErp 回调中台传回发货信息
     */
    public void dealErpShipmentInfo(String shipInfo) {
        List<YyEdiShipInfo> results;
        List<YyEdiResponseDetail> fields = Lists.newArrayList();
        List<YyEdiShipInfo> okShipInfos = Lists.newArrayList();
        YyEdiResponse error = new YyEdiResponse();
        try {
            if (StringUtils.isEmpty(shipInfo)) {
                YyEdiResponseDetail field = new YyEdiResponseDetail();
                field.setErrorCode("-100");
                field.setErrorMsg("发货信息空");
                fields.add(field);
                error.setFields(fields);
                String reason = JsonMapper.nonEmptyMapper().toJson(error);
                throw new OPServerException(200, reason);
            }
            results = JsonMapper.nonEmptyMapper().fromJson(shipInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YyEdiShipInfo.class));
            fields = Lists.newArrayList();
            int count = 0;
            for (YyEdiShipInfo yyEdiShipInfo : results) {
                try {
                    String shipmentCode = yyEdiShipInfo.getShipmentId();
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
                    //判断状态及获取接下来的状态
                    Flow flow = flowPicker.pickShipments();
                    OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
                    if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {

                        log.error("shipment(id={})'s status({}) not fit for ship",
                                shipment.getId(), shipment.getStatus());
                        if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())
                                || Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                                || Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())) {
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("300");
                            field.setErrorMsg("已经发货完成，请勿再次发货");
                            fields.add(field);
                        } else {
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("400");
                            field.setErrorMsg("发货单状态异常");
                            fields.add(field);
                        }
                        count++;
                        continue;
                    }

                    //校验物流公司是否正确
                    orderReadLogic.makeExpressNameByhkCode(yyEdiShipInfo.getShipmentCorpCode());


                    //校验成功，直接转存至okShipInfos
                    okShipInfos.add(yyEdiShipInfo);
                } catch (Exception e) {
                    log.error("update shipment failed,shipment id is {},caused by {}", yyEdiShipInfo.getShipmentId(), Throwables.getStackTraceAsString(e));
                    YyEdiResponseDetail field = new YyEdiResponseDetail();
                    field.setShipmentId(yyEdiShipInfo.getShipmentId());
                    field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                    field.setErrorCode("-100");
                    field.setErrorMsg(e.getMessage());
                    fields.add(field);
                    count++;
                    continue;

                }
                YyEdiResponseDetail field = new YyEdiResponseDetail();
                field.setShipmentId(yyEdiShipInfo.getShipmentId());
                field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                field.setErrorCode("200");
                field.setErrorMsg("");
                fields.add(field);
            }
            if (CollectionUtils.isNotEmpty(okShipInfos)) {
                Response<Long> response = receiveYyediResultLogic.createShipmentResultTask(okShipInfos);
                if (!response.isSuccess()) {
                    log.error("yyEDI.shipments.api.createShipmentResultTask.failed,caused by {}", response.getError());
                    throw new ServiceException("yyEDI.shipments.api.createShipmentResultTask.failed");
                }
            }
            if (count > 0) {
                throw new ServiceException("shipment.receive.shipinfo.failed");
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("yyedi shipment handle result to pousheng fail,error:{}", Throwables.getStackTraceAsString(e));
            error.setFields(fields);
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            throw new OPServerException(200, reason);
        } catch (Exception e) {
            log.error("yyedi shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
            error.setFields(fields);
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            throw new OPServerException(200, reason);
        }
    }


    /**
     * yyEDi回传售后单信息
     *
     * @param refundOrderId
     * @param yyEDIRefundOrderId
     * @param receivedDate
     * @param itemInfo
     */
    @OpenMethod(key = "yyEDI.refund.confirm.received.api", paramNames = {"refundOrderId", "yyEDIRefundOrderId", "itemInfo",
            "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(String refundOrderId,
                                   @NotEmpty(message = "yy.refund.order.id.is.null") String yyEDIRefundOrderId,
                                   @NotEmpty(message = "itemInfo.is.null") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate) {
        log.info("YYEDI-SYNC-REFUND-STATUS-START param refundOrderId is:{} yyediRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);

        dealErpRefundInfo(refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);

        log.info("YYEDI-SYNC-REFUND-STATUS-END param refundOrderId is:{} yyediRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);

    }


    @OpenMethod(key = "yj.refund.confirm.received.api", paramNames = {"refundOrderId", "yjRefundOrderId", "itemInfo",
            "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncYJERPRefundStatus(String refundOrderId,
                                      @NotEmpty(message = "yj.refund.order.id.is.null") String yjRefundOrderId,
                                      @NotEmpty(message = "itemInfo.is.null") String itemInfo,
                                      @NotEmpty(message = "received.date.empty") String receivedDate) {
        log.info("YJERP-SYNC-REFUND-STATUS-START param refundOrderId is:{} yjRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yjRefundOrderId, itemInfo, receivedDate);

        dealErpRefundInfo(refundOrderId, yjRefundOrderId, itemInfo, receivedDate);

        log.info("YJERP-SYNC-REFUND-STATUS-END param refundOrderId is:{} yjRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yjRefundOrderId, itemInfo, receivedDate);

    }


    public void dealErpRefundInfo(String refundOrderId, String erpRefundOrderId, String itemInfo, String receivedDate) {

        YyEdiResponse error = new YyEdiResponse();
        try {
            List<YYEdiRefundConfirmItem> items = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YYEdiRefundConfirmItem.class));
            if (refundOrderId == null) {
                return;
            }
            Refund refund = refundReadLogic.findRefundByRefundCode(refundOrderId);
            DateTime dt = DateTime.parse(receivedDate, DFT);
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setIslock(0);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            refundExtra.setYyediRefundId(erpRefundOrderId);
            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                error.setErrorCode("300");
                error.setErrorMsg("已经通知中台退货，请勿再次通知");
                throw new ServiceException(updateStatusRes.getError());
            }
            //异步任务
            Map<String, String> extra = refund.getExtra();
            extra.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            extra.put(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO, mapper.toJson(items));
            refund.setExtraJson(mapper.toJson(extra));
            Response<Long> response = receiveYyediResultLogic.createRefundStatusTask(Lists.newArrayList(refund));
            if (!response.isSuccess()) {
                log.error("yyEDI.refund.confirm.received.api.failed,caused by {}", response.getError());
                throw new ServiceException("yyEDI.refund.confirm.received.api.failed");
            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("yyedi shipment handle result to pousheng fail,error:{}", Throwables.getStackTraceAsString(e));
            if (Objects.nonNull(error) && Objects.nonNull(error.getErrorCode())) {
                String reason = JsonMapper.nonEmptyMapper().toJson(error);
                throw new OPServerException(200, reason);
            } else {
                error.setErrorCode("-100");
                error.setErrorMsg(e.getMessage());
                String reason = JsonMapper.nonEmptyMapper().toJson(error);
                throw new OPServerException(200, reason);
            }
        } catch (Exception e) {
            log.error("yyedi shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
            error.setErrorCode("-100");
            error.setErrorMsg(e.getMessage());
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            throw new OPServerException(200, reason);
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
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            case REJECT_GOODS:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }

    /**
     * @param shipInfo wms 回调中台传回发货信息
     */
    public void dealWmsShipmentInfo(String shipInfo) {
        List<YyEdiShipInfo> results = null;
        List<YyEdiResponseDetail> fields = Lists.newArrayList();
        List<YyEdiShipInfo> okShipInfos = Lists.newArrayList();
        YyEdiResponse error = new YyEdiResponse();
        try {
            if (StringUtils.isEmpty(shipInfo)) {
                YyEdiResponseDetail field = new YyEdiResponseDetail();
                field.setErrorCode("-100");
                field.setErrorMsg("发货信息空");
                fields.add(field);
                error.setFields(fields);
                String reason = JsonMapper.nonEmptyMapper().toJson(error);
                throw new OPServerException(200, reason);
            }
            results = JsonMapper.nonEmptyMapper().fromJson(shipInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YyEdiShipInfo.class));
            fields = Lists.newArrayList();
            int count = 0;
            for (YyEdiShipInfo yyEdiShipInfo : results) {
                try {
                    String shipmentCode = yyEdiShipInfo.getShipmentId();
                    validateWmsShipment(yyEdiShipInfo, fields);

                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);

                    if (yyEdiShipInfo.getItemInfos() != null) {
                        Integer size = yyEdiShipInfo.getItemInfos().stream().filter(e -> e.getQuantity() > 0).collect(Collectors.toList()).size();
                        if (size == 0) {
                            Map<String, String> extraMap = shipment.getExtra();
                            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                            shipmentExtra.setRemark("物流整单缺货");

                            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                            shipment.setExtra(extraMap);
                            Response<Boolean> response = shipmentWriteService.update(shipment);
                            if (!response.isSuccess()) {
                                log.error("wms.shipments.api update shipment fail ,caused by {}", response.getError());
                                throw new ServiceException(response.getError());
                            }
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
                            field.setErrorCode("-100");
                            field.setErrorMsg("物流整单缺货");
                            fields.add(field);
                            count++;
                            continue;
                        }
                    }
                    //判断状态及获取接下来的状态
                    Flow flow = flowPicker.pickShipments();
                    OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
                    if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {

                        log.error("shipment(id={})'s status({}) not fit for ship",
                                shipment.getId(), shipment.getStatus());
                        if (Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue())
                                || Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                                || Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())) {
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("300");
                            field.setErrorMsg("已经发货完成，请勿再次发货");
                            fields.add(field);
                        } else {
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("400");
                            field.setErrorMsg("发货单状态异常");
                            fields.add(field);
                        }
                        count++;
                        continue;
                    }
                    //校验成功，直接转存至okShipInfos
                    okShipInfos.add(yyEdiShipInfo);
                } catch (Exception e) {
                    log.error("update shipment failed,shipment id is {},caused by {}", yyEdiShipInfo.getShipmentId(), Throwables.getStackTraceAsString(e));
                    YyEdiResponseDetail field = new YyEdiResponseDetail();
                    field.setShipmentId(yyEdiShipInfo.getShipmentId());
                    field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                    field.setErrorCode("-100");
                    field.setErrorMsg(e.getMessage());
                    fields.add(field);
                    count++;
                    continue;

                }
                YyEdiResponseDetail field = new YyEdiResponseDetail();
                field.setShipmentId(yyEdiShipInfo.getShipmentId());
                field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                field.setErrorCode("200");
                field.setErrorMsg("");
                fields.add(field);
            }
            if (CollectionUtils.isNotEmpty(okShipInfos)) {
                Response<Long> response = receiveYyediResultLogic.createShipmentResultTask(okShipInfos);
                if (!response.isSuccess()) {
                    log.error("wms.shipments.api.createShipmentResultTask.failed,caused by {}", response.getError());
                    throw new ServiceException("yyEDI.shipments.api.createShipmentResultTask.failed");
                }
            }
            if (count > 0) {
                throw new ServiceException("shipment.receive.shipinfo.failed");
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("wms shipment handle result to pousheng fail,error:{}", Throwables.getStackTraceAsString(e));
            error.setFields(fields);
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            throw new OPServerException(200, reason);
        } catch (Exception e) {
            log.error("wms shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
            error.setFields(fields);
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            throw new OPServerException(200, reason);
        }
    }

    /**
     * 验证报文必填字段
     *
     * @param shipInfo
     * @param fields
     */
    private void validateWmsShipment(YyEdiShipInfo shipInfo, List<YyEdiResponseDetail> fields) {
        if (shipInfo == null) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment info required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getYyEDIShipmentId())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment yyEDIShipmentId required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getShipmentCorpCode())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment shipmentCorpCode required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getShipmentDate())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment shipmentDate required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getShipmentSerialNo())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment shipmentSerialNo required");
        }


        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getCardRemark())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment cardRemark required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getTransportMethodCode())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment transportMethodCode required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getTransportMethodName())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment transportMethodName required");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(shipInfo.getExpectDate())) {
            throwValidateFailedException(shipInfo.getShipmentId(), fields, "shipment expectDate required");
        }

        for (YyEdiShipInfo.ItemInfo itemInfo : shipInfo.getItemInfos()) {
            if (org.apache.commons.lang3.StringUtils.isBlank(itemInfo.getSkuCode())) {
                throwValidateFailedException(shipInfo.getShipmentId(), fields, "itemInfo skuCode required");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(itemInfo.getShipmentCorpCode())) {
                throwValidateFailedException(shipInfo.getShipmentId(), fields, "itemInfo shipmentCorpCode required");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(itemInfo.getShipmentSerialNo())) {
                throwValidateFailedException(shipInfo.getShipmentId(), fields, "itemInfo shipmentSerialNo required");
            }
            if (itemInfo.getQuantity() == null) {
                throwValidateFailedException(shipInfo.getShipmentId(), fields, "itemInfo quantity required");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(itemInfo.getBoxNo())) {
                throwValidateFailedException(shipInfo.getShipmentId(), fields, "itemInfo boxNo required");
            }
        }
    }

    /**
     * 抛报文验证失败异常
     *
     * @param shipmentId
     * @param fields
     * @param message
     */
    private void throwValidateFailedException(String shipmentId, List<YyEdiResponseDetail> fields, String message) {
        YyEdiResponseDetail field = new YyEdiResponseDetail();
        field.setShipmentId(shipmentId);
        field.setErrorCode("-100");
        field.setErrorMsg(message);
        fields.add(field);
        throw new ServiceException(message);
    }
}
