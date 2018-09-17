package com.pousheng.middle.open.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiResponse;
import com.pousheng.middle.open.api.dto.YyEdiResponseDetail;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.web.order.component.*;
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
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
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
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private ReceiveYyediResultLogic receiveYyediResultLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

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

    /**
     * yjERP回传发货信息
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
     * @param shipInfo
     */
    @OpenMethod(key = "jit.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveJitShipmentResult(String shipInfo) {
        log.info("WMS-JIT-SHIPMENT-INFO-START param: shipInfo [{}]", shipInfo);
        dealWmsShipmentInfo(shipInfo);
        log.info("WMS-JIT-SHIPMENT-INFO-END param: shipInfo [{}]", shipInfo);
    }


    /**
     *
     * @param shipInfo yyEdi or yjErp 回调中台传回发货信息
     */
    public void dealErpShipmentInfo (String shipInfo) {
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
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
                    if (yyEdiShipInfo.getItemInfos() != null) {
                        Integer size = yyEdiShipInfo.getItemInfos().stream().filter(e -> e.getQuantity() > 0).collect(Collectors.toList()).size();
                        if (size == 0) {
                            Map<String,String> extraMap = shipment.getExtra();
                            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                            shipmentExtra.setRemark("物流整单缺货");
                            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                            shipment.setExtra(extraMap);
                            Response<Boolean> response = shipmentWriteService.update(shipment);
                            if (!response.isSuccess()) {
                                log.error("yyEDI.shipments.api update shipment fail ,caused by {}", response.getError());
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
                                ||Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                                ||Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())){
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("300");
                            field.setErrorMsg("已经发货完成，请勿再次发货");
                            fields.add(field);
                        }else{
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


    public void dealErpRefundInfo (String refundOrderId, String erpRefundOrderId, String itemInfo, String receivedDate) {

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
     *
     * @param shipInfo wms 回调中台传回发货信息
     */
    public void dealWmsShipmentInfo (String shipInfo) {
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
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);

                    validateWmsShipment(yyEdiShipInfo,fields);
                    if (yyEdiShipInfo.getItemInfos() != null) {
                        Integer size = yyEdiShipInfo.getItemInfos().stream().filter(e -> e.getQuantity() > 0).collect(Collectors.toList()).size();
                        if (size == 0) {
                            Map<String,String> extraMap = shipment.getExtra();
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
                            ||Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CONFIRMD_SUCCESS.getValue())
                            ||Objects.equals(shipment.getStatus(),MiddleShipmentsStatus.CONFIRMED_FAIL.getValue())){
                            YyEdiResponseDetail field = new YyEdiResponseDetail();
                            field.setShipmentId(yyEdiShipInfo.getShipmentId());
                            field.setYyEdiShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));
                            field.setErrorCode("300");
                            field.setErrorMsg("已经发货完成，请勿再次发货");
                            fields.add(field);
                        }else{
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
     * @param shipInfo
     * @param fields
     */
    private void validateWmsShipment(YyEdiShipInfo shipInfo,List<YyEdiResponseDetail> fields){
        if(shipInfo==null){
            throwValidateFailedException("",fields,"shipment info required");
        }
        if(StringUtils.isEmpty(shipInfo.getCardRemark())){
            throwValidateFailedException("",fields,"cardRemark required");
        }
        if(StringUtils.isEmpty(shipInfo.getTransportMethodCode())){
            throwValidateFailedException("",fields,"transportMethodCode required");
        }
        if(StringUtils.isEmpty(shipInfo.getTransportMethodName())){
            throwValidateFailedException("",fields,"transportMethodName required");
        }
        if(StringUtils.isEmpty(shipInfo.getExpectDate())){
            throwValidateFailedException("",fields,"expectDate required");
        }
    }

    /**
     * 抛报文验证失败异常
     * @param shipmentId
     * @param fields
     * @param message
     */
    private void throwValidateFailedException(String shipmentId,List<YyEdiResponseDetail> fields,String message){
        YyEdiResponseDetail field = new YyEdiResponseDetail();
        field.setShipmentId(shipmentId);
        field.setErrorCode("200");
        field.setErrorMsg("");
        fields.add(field);
        throw new ServiceException(message);
    }
}
