package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.HkConfirmReturnItemInfo;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfo;
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
    private ShipmentWiteLogic shipmentWiteLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * yyEDI回传发货信息
     * @param shipInfo
     */
    @OpenMethod(key = "yyEDI.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentResult(List<YyEdiShipInfo> shipInfo){
       try{
        log.info("YYEDI-SHIPMENT-INFO-start param=======>{}",shipInfo);
        for (YyEdiShipInfo yyEdiShipInfo:shipInfo){

            try{
                DateTime dt = DateTime.parse(yyEdiShipInfo.getShipmentDate(), DFT);
                Long shipmentId = yyEdiShipInfo.getShipmentId();
                Shipment shipment  = shipmentReadLogic.findShipmentById(shipmentId);
                //判断状态及获取接下来的状态
                Flow flow = flowPicker.pickShipments();
                OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
                if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                    log.error("shipment(id={})'s status({}) not fit for ship",
                            shipment.getId(), shipment.getStatus());
                    throw new ServiceException("shipment.current.status.not.allow.ship");
                }
                Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

                if (!Objects.equals(yyEdiShipInfo.getYyEDIShipmentId(), shipmentExtra.getOutShipmentId())) {
                    log.error("yyedi shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", yyEdiShipInfo.getYyEDIShipmentId(), shipment.getId(), shipmentExtra.getOutShipmentId());
                    throw new ServiceException("yyedi.shipment.id.not.matching");
                }


                //封装更新信息
                Shipment update = new Shipment();
                update.setId(shipment.getId());
                Map<String, String> extraMap = shipment.getExtra();
                shipmentExtra.setShipmentSerialNo(yyEdiShipInfo.getShipmentSerialNo());
                shipmentExtra.setShipmentCorpCode(yyEdiShipInfo.getShipmentCorpCode());
                //通过恒康代码查找快递名称
                ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(yyEdiShipInfo.getShipmentCorpCode());
                shipmentExtra.setShipmentCorpName(expressCode.getName());
                shipmentExtra.setShipmentDate(dt.toDate());
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                update.setExtra(extraMap);

                //更新状态
                Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
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
            }catch (Exception e){
                log.error("");
                continue;
            }
        }
       }catch (JsonResponseException | ServiceException e) {
        log.error("hk shipment handle result to pousheng fail,error:{}", e.getMessage());
        throw new OPServerException(200,e.getMessage());
       }catch (Exception e){
        log.error("hk shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
        throw new OPServerException(200,"sync.fail");
       }
    }

    /**
     * yyEDi回传售后单信息
     * @param refundOrderId
     * @param yyEDIRefundOrderId
     * @param receivedDate
     * @param itemInfo
     */
    @OpenMethod(key = "yyEDI.refund.confirm.received.api", paramNames = {"refundOrderId", "yyEDIRefundOrderId", "itemInfo",
            "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(Long refundOrderId,
                                   @NotEmpty(message = "yy.refund.order.id.is.null") String yyEDIRefundOrderId,
                                   @NotEmpty(message = "received.date.empty") String receivedDate,
                                   List<YYEdiRefundConfirmItem> itemInfo
    ) {
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);
        try {
            if (refundOrderId == null) {
                return;
            }
            Refund refund = refundReadLogic.findRefundById(refundOrderId);
            Map<String, String> extraMap = refund.getExtra();
            String hkRefundId = extraMap.get(TradeConstants.HK_REFUND_ID);
            DateTime dt = DateTime.parse(receivedDate, DFT);
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
          /*  List<YYEdiRefundConfirmItem> hkConfirmReturnItemInfos = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YYEdiRefundConfirmItem.class));

           // refundExtra.setHkConfirmItemInfos(hkConfirmReturnItemInfos);
*/
            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新扩展信息
            Refund update = new Refund();
            update.setId(refundOrderId);
            Map<String, String> extra = refund.getExtra();
            extra.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            update.setExtra(extra);

            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update rMatrixRequestHeadefund(id:{}) extra:{} fail,error:{}", refundOrderId, refundExtra, updateExtraRes.getError());
                //这就就不抛出错了，中台自己处理即可。
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("hk shipment handle result to pousheng fail,error:{}", e.getMessage());
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
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
            case AFTER_SALES_REFUND:
                log.error("refund(id:{}) type:{} not allow hk confirm", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.not.allow.hk.confirm");
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }

}
