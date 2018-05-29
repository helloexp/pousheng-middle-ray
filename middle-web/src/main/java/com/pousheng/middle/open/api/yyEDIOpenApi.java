package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiResponse;
import com.pousheng.middle.open.api.dto.YyEdiResponseDetail;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private ReceiveYyediResultLogic receiveYyediResultLogic;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * yyEDI回传发货信息
     *
     * @param shipInfo
     */
    @OpenMethod(key = "yyEDI.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentResult(String shipInfo) {
        List<YyEdiShipInfo> results = null;
        List<YyEdiResponseDetail> fields = Lists.newArrayList();
        List<YyEdiShipInfo> okShipInfos = Lists.newArrayList();
        YyEdiResponse error = new YyEdiResponse();
        try {
            log.info("YYEDI-SHIPMENT-INFO-start param=======>{}", shipInfo);
            results = JsonMapper.nonEmptyMapper().fromJson(shipInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YyEdiShipInfo.class));
            fields = Lists.newArrayList();
            int count = 0;
            for (YyEdiShipInfo yyEdiShipInfo : results) {
                try {
                    DateTime dt = new DateTime();
                    String shipmentCode = yyEdiShipInfo.getShipmentId();
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
                    //判断状态及获取接下来的状态
                    Flow flow = flowPicker.pickShipments();
                    OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
                    if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                        log.error("shipment(id={})'s status({}) not fit for ship",
                                shipment.getId(), shipment.getStatus());
                        YyEdiResponseDetail field = new YyEdiResponseDetail();
                        field.setShipmentId(yyEdiShipInfo.getShipmentId());
                        field.setYyEdiShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
                        field.setErrorCode("300");
                        field.setErrorMsg("已经发货完成，请勿再次发货");
                        fields.add(field);
                        count++;
                        continue;
                    }
                    //校验成功，直接转存至okShipInfos
                    okShipInfos.add(yyEdiShipInfo);

                    // Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
                    // //更新状态
                    // Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
                    // if (!updateStatusRes.isSuccess()) {
                    //     log.error("update shipment(id:{}) status to :{} fail,error:{}", shipment.getId(), targetStatus, updateStatusRes.getError());
                    //     throw new OPServerException(200, updateStatusRes.getError());
                    // }
                    // ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                    // //封装更新信息
                    // Shipment update = new Shipment();
                    // update.setId(shipment.getId());
                    // Map<String, String> extraMap = shipment.getExtra();
                    // shipmentExtra.setShipmentSerialNo(yyEdiShipInfo.getShipmentSerialNo());
                    // shipmentExtra.setShipmentCorpCode(yyEdiShipInfo.getShipmentCorpCode());
                    // if (Objects.isNull(yyEdiShipInfo.getWeight())) {
                    //     shipmentExtra.setWeight(0L);
                    // } else {
                    //     shipmentExtra.setWeight(yyEdiShipInfo.getWeight());
                    // }
                    // //通过恒康代码查找快递名称
                    // ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(yyEdiShipInfo.getShipmentCorpCode());
                    // shipmentExtra.setShipmentCorpName(expressCode.getName());
                    // shipmentExtra.setShipmentDate(dt.toDate());
                    // shipmentExtra.setOutShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
                    // extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                    // update.setExtra(extraMap);
                    // //更新基本信息
                    // Response<Boolean> updateRes = shipmentWriteService.update(update);
                    // if (!updateRes.isSuccess()) {
                    //     log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
                    //     throw new OPServerException(200, updateRes.getError());
                    // }
                    //
                    // //同步pos单到恒康
                    // Map<String, Object> param = Maps.newHashMap();
                    // param.put(TradeConstants.SHIPMENT_ID, shipment.getId());
                    // autoCompensateLogic.createAutoCompensationTask(param, TradeConstants.YYEDI_SHIP_NOTIFICATION, null);
                } catch (Exception e) {
                    log.error("update shipment failed,shipment id is {},caused by {}", yyEdiShipInfo.getShipmentId(), e.getMessage());
                    YyEdiResponseDetail field = new YyEdiResponseDetail();
                    field.setShipmentId(yyEdiShipInfo.getShipmentId());
                    field.setYyEdiShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
                    field.setErrorCode("-100");
                    field.setErrorMsg(e.getMessage());
                    fields.add(field);
                    count++;
                    continue;

                }
                YyEdiResponseDetail field = new YyEdiResponseDetail();
                field.setShipmentId(yyEdiShipInfo.getShipmentId());
                field.setYyEdiShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
                field.setErrorCode("200");
                field.setErrorMsg("");
                fields.add(field);
            }
            if (count > 0) {
                throw new ServiceException("shipment.receive.shipinfo.failed");
            }
            if(CollectionUtils.isNotEmpty(okShipInfos)){
                Response<Long> response = receiveYyediResultLogic.createShipmentResultTask(okShipInfos);
                if(!response.isSuccess()){
                    log.error("yyEDI.shipments.api.createShipmentResultTask.failed,caused by {}",response.getError());
                    throw new ServiceException("yyEDI.shipments.api.createShipmentResultTask.failed");
                }
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("yyedi shipment handle result to pousheng fail,error:{}", e.getMessage());
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
                                   @NotEmpty(message = "received.date.empty") String receivedDate
    ) {
        log.info("YYEDI-SYNC-REFUND-STATUS-START param refundOrderId is:{} yyediRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);
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
            refundExtra.setYyediRefundId(yyEDIRefundOrderId);
            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                error.setErrorCode("300");
                error.setErrorMsg("已经通知中台退货，请勿再次通知");
                throw new ServiceException(updateStatusRes.getError());
            }
            //更新扩展信息
            Refund update = new Refund();
            update.setId(refund.getId());
            Map<String, String> extra = refund.getExtra();
            extra.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            extra.put(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO, mapper.toJson(items));
            update.setExtra(extra);

            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update rMatrixRequestHeadefund(id:{}) extra:{} fail,error:{}", refundOrderId, refundExtra, updateExtraRes.getError());
            }
            //同步pos单到恒康
            //判断pos单是否需要同步恒康,如果退货仓数量全是0
            if (validateYYConfirmedItems(items)) {
                try {
                    Response<Boolean> r = syncRefundPosLogic.syncRefundPosToHk(refund);
                    if (!r.isSuccess()) {
                        Map<String, Object> param1 = Maps.newHashMap();
                        param1.put("refundId", refund.getId());
                        autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK, r.getError());
                    }
                } catch (Exception e) {
                    Map<String, Object> param1 = Maps.newHashMap();
                    param1.put("refundId", refund.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK, e.getMessage());
                }
            }
            //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
            refundWriteLogic.getThirdRefundResult(refund);
        } catch (JsonResponseException | ServiceException e) {
            log.error("yyedi shipment handle result to pousheng fail,error:{}", e.getMessage());
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
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }

    private boolean validateYYConfirmedItems(List<YYEdiRefundConfirmItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        } else {
            int count = 0;
            for (YYEdiRefundConfirmItem item : items) {
                if (Objects.equals(item.getQuantity(), "0")) {
                    count++;
                }
            }
            if (count == items.size()) {
                return false;
            } else {
                return true;
            }
        }
    }

}
