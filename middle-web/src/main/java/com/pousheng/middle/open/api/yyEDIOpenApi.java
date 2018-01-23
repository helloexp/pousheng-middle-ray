package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
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
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
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
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private EventBus eventBus;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * yyEDI回传发货信息
     * @param shipInfo
     */
    @OpenMethod(key = "yyEDI.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentResult(String shipInfo){
       try{
        log.info("YYEDI-SHIPMENT-INFO-start param=======>{}",shipInfo);
        List<YyEdiShipInfo> results = JsonMapper.nonEmptyMapper().fromJson(shipInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class,YyEdiShipInfo.class));
        for (YyEdiShipInfo yyEdiShipInfo:results){

            try{
                DateTime dt = new DateTime();
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
                shipmentExtra.setOutShipmentId(yyEdiShipInfo.getYyEDIShipmentId());
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
                //同步pos单到恒康
                Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
                if(!response.isSuccess()){
                    Map<String,Object> param1 = Maps.newHashMap();
                    param1.put("shipmentId",shipment.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1,TradeConstants.FAIL_SYNC_POS_TO_HK);
                }
            }catch (Exception e){
                log.error("update shipment failed,shipment id is {},caused by {}",yyEdiShipInfo.getShipmentId(),e.getMessage());
                continue;
            }
        }
       }catch (JsonResponseException | ServiceException e) {
        log.error("yyedi shipment handle result to pousheng fail,error:{}", e.getMessage());
        throw new OPServerException(200,e.getMessage());
       }catch (Exception e){
        log.error("yyedi shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
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
                                   String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate
                                   ) {
        log.info("YYEDI-SYNC-REFUND-STATUS-START param refundOrderId is:{} yyediRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, yyEDIRefundOrderId, itemInfo, receivedDate);
        try {
            List<YYEdiRefundConfirmItem> results = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class,YYEdiRefundConfirmItem.class));
            if (refundOrderId == null) {
                return;
            }
            Refund refund = refundReadLogic.findRefundById(refundOrderId);
            DateTime dt = DateTime.parse(receivedDate, DFT);
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            refundExtra.setYyediRefundId(yyEDIRefundOrderId);
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
            }
            //同步pos单到恒康
            try{
                Response<Boolean> r = syncRefundPosLogic.syncRefundPosToHk(refund);
                if (!r.isSuccess()){
                    Map<String,Object> param1 = Maps.newHashMap();
                    param1.put("refundId",refund.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1,TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK);
                }
            }catch (Exception e){
                Map<String,Object> param1 = Maps.newHashMap();
                param1.put("refundId",refund.getId());
                autoCompensateLogic.createAutoCompensationTask(param1,TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK);
            }
            //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
            String outId = refund.getOutId();
            if (StringUtils.hasText(outId)&&outId.contains("taobao")){
                String channel = refundReadLogic.getOutChannelTaobao(outId);
                if (Objects.equals(channel, MiddleChannel.TAOBAO.getValue())
                        &&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
                    Refund newRefund =  refundReadLogic.findRefundById(refund.getId());
                    TaobaoConfirmRefundEvent event = new TaobaoConfirmRefundEvent();
                    event.setRefundId(refund.getId());
                    event.setChannel(channel);
                    event.setOpenShopId(newRefund.getShopId());
                    event.setOpenAfterSaleId(refundReadLogic.getOutafterSaleIdTaobao(outId));
                    eventBus.post(event);
                }
            }
            //如果是苏宁的售后单，将会主动查询售后单的状态
            if (StringUtils.hasText(outId)&&outId.contains("suning")){
                String channel = refundReadLogic.getOutChannelSuning(outId);
                if (Objects.equals(channel, MiddleChannel.TAOBAO.getValue())
                        &&Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value())){
                    Refund newRefund =  refundReadLogic.findRefundById(refund.getId());
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
        } catch (JsonResponseException | ServiceException e) {
            log.error("yyedi shipment handle result to pousheng fail,error:{}", e.getMessage());
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("yyedi shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
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
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }

}
