package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.api.dto.HkHandleShipmentResult;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.HkConfirmReturnItemInfo;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
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
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private EventBus eventBus;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();




    /**
     * 恒康同步发货单处理结果
     *
     * @param data 处理结果
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipment.handle.result", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncHkHandleResult(@NotNull(message = "handle.data.is.null") String data) {
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-START results is:{} ",data);
        List<HkHandleShipmentResult> results = results = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class,HkHandleShipmentResult.class));
        try{
            for(HkHandleShipmentResult result :results){
                Long shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                String hkShipmentId = result.getHkShipmentId();
                Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);

                //冗余恒康发货单号
                //更新发货单的状态
                if (handleResult){
                    //如果发货单已受理，则跳过
                    if(shipment.getStatus()>= MiddleShipmentsStatus.WAIT_SHIP.getValue()){
                        log.warn("shipment(id:{}) duplicate request to handle,so skip",shipment.getId());
                        continue;
                    }
                    Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), MiddleShipmentsStatus.WAIT_SHIP.getValue());
                    if (!updateRes.isSuccess()) {
                        log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(),MiddleShipmentsStatus.WAIT_SHIP.getValue(), updateRes.getError());
                    }
                    //更新恒康shipmentId
                    Shipment update = new Shipment();
                    update.setId(shipment.getId());
                    ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                    shipmentExtra.setOutShipmentId(hkShipmentId);
                    Map<String, String> extraMap = shipment.getExtra();
                    extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shipmentExtra));
                    update.setExtra(extraMap);
                    log.info("start update hkShipmentId is {}",hkShipmentId);
                    shipmentWiteLogic.update(update);
                    log.info("end update hkShipmentId is {}",hkShipmentId);
                }else{
                    OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                    Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                    if (!updateSyncStatusRes.isSuccess()) {
                        log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    }
                }
            }


        }catch (JsonResponseException | ServiceException e) {
            log.error("hk shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), e.getMessage());
            throw new OPServerException(200,e.getMessage());
        }catch (Exception e){
            log.error("hk shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-END");
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
    public void syncHkShipmentStatus(@NotNull(message = "shipment.id.is.null") Long shipmentId,
                                     @NotEmpty(message = "hk.shipment.id.is.null") String hkShipmentId,
                                     @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                     @NotEmpty(message = "shipment.serial.is.empty") String shipmentSerialNo,
                                     @NotEmpty(message = "shipment.date.empty") String shipmentDate
                                     ) {
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}",
                shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
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

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, e.getMessage());
            throw new OPServerException(200,e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
    }

    /**
     * 恒康将售后单售后结果通知给中台
     * @param refundOrderId
     * @param hkRefundOrderId
     * @param itemInfo
     * @param receivedDate
     */
    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId", "hkRefundOrderId", "itemInfo",
                                                                      "receivedDate","itemCode","quantity"
                                                                      }, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(Long refundOrderId,
                                   @NotEmpty(message = "hk.refund.order.id.is.null") String hkRefundOrderId,
                                   @NotEmpty(message = "item.info.empty") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate
                                   ) {
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} receivedDate is:{} ",
                refundOrderId, hkRefundOrderId, itemInfo, receivedDate);
        try {
            if (refundOrderId==null){
                return;
            }
            Refund refund = refundReadLogic.findRefundById(refundOrderId);
            Map<String,String> extraMap = refund.getExtra();
            String hkRefundId = extraMap.get(TradeConstants.HK_REFUND_ID);
            //仅退款是没有hkRefundOrderId,所以放开这边的校验
           /* if (!Objects.equals(hkRefundOrderId, hkRefundId)) {
                log.error("hk refund id:{} not equal middle refund(id:{} ) out id:{}", hkRefundOrderId, refund.getId(), hkRefundId);
                throw new ServiceException("hk.refund.id.not.matching");
            }*/



            DateTime dt = DateTime.parse(receivedDate, DFT);
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            List<HkConfirmReturnItemInfo> hkConfirmReturnItemInfos = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class,HkConfirmReturnItemInfo.class));

            refundExtra.setHkConfirmItemInfos(hkConfirmReturnItemInfos);

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
            log.error("hk sync refund confirm to middle fail,error:{}", e.getMessage());
            throw new OPServerException(200,e.getMessage());
        } catch (Exception e) {
            log.error("hk sync refund confirm to middle fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }
        log.info("HK-SYNC-REFUND-STATUS-END");

    }


    /**
     * 恒康同步pos信息到中台
     * @param orderId  订单id
     * @param orderType 订单类型1.销售发货单,2.售后订单
     * @param posSerialNo pos单号
     * @param posType pos单类型
     * @param posAmt pos金额
     * @param posCreatedAt pos单创建时间
     */
    @OpenMethod(key = "hk.pos.api", paramNames = {"orderId", "orderType", "posSerialNo","posType",
            "posAmt","posCreatedAt"}, httpMethods = RequestMethod.POST)
    public void syncHkPosStatus(@NotNull(message = "order.id.is.null") Long orderId,
                                     @NotEmpty(message = "hk.order.type.is.null") String orderType,
                                     @NotEmpty(message = "pos.serial.is.empty")String posSerialNo,
                                     @NotNull(message = "pos.type.is.null")Integer posType,
                                     @NotNull(message = "pos.amt.is.empty")String posAmt,
                                     @NotEmpty(message = "pos.created.time.is.empty")String posCreatedAt) {
        log.info("HK-SYNC-POS-INFO-START param orderId is:{} orderType is:{}  posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{}",orderId,orderType,posSerialNo,posType,posAmt,posCreatedAt);

        try {

            DateTime dPos = DateTime.parse(posCreatedAt, DFT);
            PoushengSettlementPos pos = new PoushengSettlementPos();
            if (Objects.equals(orderType,"1")){ //pos单类型是1有两种订单类型，第一种是正常的销售发货,一种是换货生成的发货单
                OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(orderId);
                if (Objects.equals(orderShipment.getType(),1)){
                    pos.setOrderId(orderShipment.getOrderId());
                    pos.setShipType(1);
                }else{
                    pos.setOrderId(orderShipment.getAfterSaleOrderId());
                    pos.setShipType(2);
                    pos.setPosDoneAt(new Date());
                }
                String amt = String.valueOf(new BigDecimal(Double.valueOf(posAmt)*100).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(amt));
                pos.setShipmentId(orderId);
                pos.setPosType(Integer.valueOf(posType));
                pos.setPosSerialNo(posSerialNo);
                pos.setShopId(orderShipment.getShopId());
                pos.setShopName(orderShipment.getShopName());
                pos.setPosCreatedAt(dPos.toDate());


            }else if (Objects.equals(orderType,"2")){
                Refund refund = refundReadLogic.findRefundById(orderId);
                pos.setOrderId(refund.getId());
                String amt = String.valueOf(new BigDecimal(Double.valueOf(posAmt)*100).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(amt));
                pos.setPosType(Integer.valueOf(posType));
                pos.setShipType(3);
                pos.setPosSerialNo(posSerialNo);
                pos.setShopId(refund.getShopId());
                pos.setShopName(refund.getShopName());
                pos.setPosCreatedAt(dPos.toDate());
                pos.setPosDoneAt(new Date());
            }else{
                throw new ServiceException("invalid.order.type");
            }
            Response<PoushengSettlementPos> rP = poushengSettlementPosReadService.findByPosSerialNo(posSerialNo);
            if (!rP.isSuccess()){
                log.error("find pousheng settlement pos failed, posSerialNo is {},caused by {}",posSerialNo,rP.getError());
                return;
            }
            if(!Objects.isNull(rP.getResult())){
                log.error("duplicate posSerialNo is {},caused by {}",posSerialNo,rP.getError());
                return;
            }
            Response<Long> r = poushengSettlementPosWriteService.create(pos);
            if (!r.isSuccess()){
                log.error("create poushengSettlementPos failed, poushengSettlementPos:{},caused by ",pos,r.getError());
                throw new ServiceException(r.getError());
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync posInfo(id:{}) to pousheng fail,error:{}", orderId, e.getMessage());
            throw new OPServerException(200,e.getMessage());
        } catch (Exception e) {
            log.error("hk sync posInfo(id:{}) fail,orderType is ({})cause:{}", orderId,orderType, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }

        log.info("HK-SYNC-POS-INFO-END");
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
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }
}
