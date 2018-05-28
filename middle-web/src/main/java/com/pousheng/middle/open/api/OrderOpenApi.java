package com.pousheng.middle.open.api;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.dto.*;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
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

import static com.pousheng.middle.order.enums.MiddleRefundType.AFTER_SALES_RETURN;

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
        log.info("ERP-SYNC-SHIPMENT-HANDLE-RESULT-START results is:{} ", data);
        List<ErpHandleShipmentResult> results  = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class,ErpHandleShipmentResult.class));
        try{
            for(ErpHandleShipmentResult result :results){
                String shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                String erpShipmentId = result.getErpShipmentId();
                handleResult(shipmentId,handleResult,erpShipmentId);
            }

        }catch (JsonResponseException | ServiceException e) {
            log.error("erp shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), e.getMessage());
            throw new OPServerException(200,e.getMessage());
        }catch (Exception e){
            log.error("erp shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"sync.fail");
        }
        log.info("ERP-SYNC-SHIPMENT-HANDLE-RESULT-END");
    }


    /**
     * 第三方ERP同步发货完成状态到中台
     *
     * @param shipmentId       中台发货单号
     * @param shipmentId     第三方ERP发货单号
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
                                     @NotEmpty(message = "shipment.date.empty") String shipmentDate
    ) {
        this.syncHkShipmentStatus(shipmentId,erpShipmentId,shipmentCorpCode,shipmentSerialNo,shipmentDate);
    }



    /**
     * 恒康同步发货单处理结果
     *
     * @param data 处理结果
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipment.handle.result", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncHkHandleResult(@NotNull(message = "handle.data.is.null") String data) {
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-START results is:{} ",data);
        List<HkHandleShipmentResult> results  = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class,HkHandleShipmentResult.class));
        try{
            for(HkHandleShipmentResult result :results){
                String shipmentId = result.getEcShipmentId();
                Boolean handleResult = result.getSuccess();
                String hkShipmentId = result.getHkShipmentId();
                handleResult(shipmentId,handleResult,hkShipmentId);
            }

        }catch (JsonResponseException | ServiceException e) {
            log.error("hk shipment handle result, shipment(id:{}) to pousheng fail,error:{}", results.get(0).getEcShipmentId(), e.getMessage());
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk shipment handle result ,shipment(id:{}) fail,cause:{}", results.get(0).getEcShipmentId(), Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }
        log.info("HK-SYNC-SHIPMENT-HANDLE-RESULT-END");
    }

    private void handleResult(String shipmentId,Boolean handleResult,String erpShipmentId){
        Shipment shipment = null;
        try{
            shipment  = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
        }catch (Exception e){
            log.error("find shipment failed,shipment id is {} ,caused by {}",shipmentId,e.getMessage());
            return;
        }
        //冗余恒康发货单号
        //更新发货单的状态
        if (handleResult){
            //如果发货单已受理，则跳过
            if(shipment.getStatus()>= MiddleShipmentsStatus.WAIT_SHIP.getValue()){
                log.warn("shipment(id:{}) duplicate request to handle,so skip",shipment.getId());
                return;
            }
            Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentIdAndCurrentStatus(shipment.getId(),shipment.getStatus(), MiddleShipmentsStatus.WAIT_SHIP.getValue());
            if (!updateRes.isSuccess()) {
                log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(),MiddleShipmentsStatus.WAIT_SHIP.getValue(), updateRes.getError());
            }
            //更新恒康shipmentId
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            shipmentExtra.setOutShipmentId(erpShipmentId);
            Map<String, String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shipmentExtra));
            update.setExtra(extraMap);
            log.info("start update erpShipmentId is {}",erpShipmentId);
            shipmentWiteLogic.update(update);
            log.info("end update erpShipmentId is {}",erpShipmentId);
        }else{
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
                        "shipmentSerialNo is:{} shipmentDate is:{}",
                shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            Shipment shipment = null;
            try{
                shipment  = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
            }catch (Exception e){
                log.error("find shipment failed,shipment id is {} ,caused by {}",shipmentId,e.getMessage());
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
            Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentIdAndCurrentStatus(shipment.getId(),shipment.getStatus(), targetStatus);
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
            if (!response.isSuccess()) {
                Map<String, Object> param1 = Maps.newHashMap();
                param1.put("shipmentId", shipment.getId());
                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());

            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, e.getMessage());
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
    }

    /**
     * skx将售后单售后结果通知给中台
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
        syncHkRefundStatus(refundOrderId,erpRefundOrderId,itemInfo,receivedDate);

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
            refundExtra.setIslock(1);
            List<HkConfirmReturnItemInfo> hkConfirmReturnItemInfos = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class,HkConfirmReturnItemInfo.class));

            refundExtra.setHkConfirmItemInfos(hkConfirmReturnItemInfos);

            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                throw new OPServerException(200,"已经通知中台退货，请勿再次通知");
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
            if(refund.getShopName().startsWith("mpos")){
               syncMposOrderLogic.notifyMposRefundReceived(refund.getOutId());
            }

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

            //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
            refundWriteLogic.getThirdRefundResult(refund);

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
     *
     * @param orderId      订单id
     * @param orderType    订单类型1.销售发货单,2.售后订单
     * @param posSerialNo  pos单号
     * @param posType      pos单类型
     * @param posAmt       pos金额
     * @param posCreatedAt pos单创建时间
     */
    @OpenMethod(key = "hk.pos.api", paramNames = {"orderId", "orderType", "posSerialNo","posType",
            "posAmt","posCreatedAt"}, httpMethods = RequestMethod.POST)
    public void syncHkPosStatus(@NotNull(message = "order.id.is.null") String orderId,
                                     @NotEmpty(message = "hk.order.type.is.null") String orderType,
                                     @NotEmpty(message = "pos.serial.is.empty")String posSerialNo,
                                     @NotNull(message = "pos.type.is.null")Integer posType,
                                     @NotNull(message = "pos.amt.is.empty")String posAmt,
                                     @NotEmpty(message = "pos.created.time.is.empty")String posCreatedAt) {
        log.info("HK-SYNC-POS-INFO-START param orderId is:{} orderType is:{}  posSerialNo is:{} posType is:{} posAmt is:{} posCreatedAt is:{}",orderId,orderType,posSerialNo,posType,posAmt,posCreatedAt);

        try {

            DateTime dPos = DateTime.parse(posCreatedAt, DFT);
            PoushengSettlementPos pos = new PoushengSettlementPos();
            if (Objects.equals(orderType, "1")) { //pos单类型是1有两种订单类型，第一种是正常的销售发货,一种是换货生成的发货单
                OrderShipment orderShipment = null;
                try{
                    orderShipment = shipmentReadLogic.findOrderShipmentByShipmentCode(orderId);

                } catch (Exception e) {
                    log.error("find order shipment failed,shipment id is {} ,caused by {}", orderId, e.getMessage());
                    return;
                }
                if (Objects.equals(orderShipment.getType(),1)){
                    pos.setOrderId(orderShipment.getOrderCode());
                    pos.setShipType(1);
                }else{
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
                try{
                    refund = refundReadLogic.findRefundByRefundCode(orderId);
                }catch (Exception e){
                    log.error("find refund failed,refund id is {} ,caused by {}",orderId,e.getMessage());
                    return;
                }
                pos.setOrderId(refund.getRefundCode());
                String amt = String.valueOf(new BigDecimal(Double.valueOf(posAmt)*100).setScale(0, RoundingMode.HALF_DOWN));
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
            log.error("hk sync posInfo(id:{}) to pousheng fail,error:{}", orderId, e.getMessage());
            throw new OPServerException(200, e.getMessage());
        } catch (Exception e) {
            log.error("hk sync posInfo(id:{}) fail,orderType is ({})cause:{}", orderId, orderType, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "sync.fail");
        }

        log.info("HK-SYNC-POS-INFO-END");
    }


    /**
     * 取消订单
     *
     * @param data 处理结果
     * @return 是否同步成功
     */
    @OpenMethod(key = "out.order.cancel.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public void syncOrderCancel(@NotNull(message = "cancel.data.is.null") String data) {
        log.info("SYNC-OUT-ORDER-CANCEL-START DATA is:{} ", data);
        CancelOutOrderInfo cancelOutOrderInfo = JSON_MAPPER.fromJson(data, CancelOutOrderInfo.class);

        String outId = cancelOutOrderInfo.getOutOrderId();
        String outFrom = cancelOutOrderInfo.getChannel();
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
        orderWriteLogic.autoCancelShopOrder(shopOrderOptional.get().getId());


        log.info("cancelOutOrderInfo:", cancelOutOrderInfo);
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

        log.info("out.order.refund.api req={}",data);
        OutOrderApplyRefund applyRefund = JSON_MAPPER.fromJson(data, OutOrderApplyRefund.class);
        if (Objects.isNull(applyRefund)) {
            throw new OPServerException(200, "parameter deserialize failed");
        }
        OutRefundOrder refundOrder = applyRefund.getRefund();
        if (Objects.isNull(refundOrder)) {
            throw new OPServerException(200, "parameter rufund can't be empty");
        }
        if (CollectionUtils.isEmpty(applyRefund.getItems())){
            throw new OPServerException(200, "items can't be empty");
        }
        String outId = refundOrder.getOutOrderId();
        String outFrom = refundOrder.getChannel();
        Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(outId, outFrom);
        if (!findShopOrder.isSuccess()||!findShopOrder.getResult().isPresent()) {
            log.error("fail to find shop order by outId={},outFrom={} when sync receiver info,cause:{}",
                    outId, outFrom, findShopOrder.getError());
            throw new OPServerException(200, findShopOrder.getError());
        }
        ShopOrder shopOrder = findShopOrder.getResult().get();

        SubmitRefundInfo refundInfo = new SubmitRefundInfo();

        Response<List<OrderShipment>> response = orderShipmentReadService.findByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()||CollectionUtils.isEmpty(response.getResult())) {
            log.error("fail to find OrderShipmentv  by orderId={},OrderLevel={} when sync receiver info,cause:{}",
                    shopOrder.getId(), OrderLevel.SHOP, response.getError());
            throw new OPServerException(200, findShopOrder.getError());
        }
        refundInfo.setOrderId(shopOrder.getId());
        OrderShipment shipment = response.getResult().get(0);
        refundInfo.setOutAfterSaleOrderId(refundOrder.getOutAfterSaleOrderId()); //售后单id
        refundInfo.setShipmentId(shipment.getShipmentId());
        refundInfo.setRefundType(AFTER_SALES_RETURN.value()); //退售后 类型
        refundInfo.setFee(refundOrder.getFee()); //金额
        ArrayList<EditSubmitRefundItem> refundItems = Lists.newArrayList();
        applyRefund.getItems().forEach(x->{
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
        refundInfo.setOperationType(1); //操作类型 //这里不需要同步给yyedi,需要客服审核手动触发推送给yyedi
        refundInfo.setReturnStockid(refundOrder.getReturnStockid()); //退货仓id 文案有点问题存到warehousId
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
    public void cancelRefundOrder(@NotNull(message = "cancel.refund.data.is.null") String data) {
        log.info("SYNC-OUT-REFUND-CANCEL-START DATA is:{} ", data);

        CancelOutRefundInfo cancelOutRefundInfo = JSON_MAPPER.fromJson(data, CancelOutRefundInfo.class);
        String outerRefundId = cancelOutRefundInfo.getOutAfterSaleOrderId();
        Refund refund;
        try {
            refund = refundReadLogic.findRefundByOutId(outerRefundId);
            refund.setRefundType(MiddleRefundType.AFTER_SALES_RETURN.value());//退货退款
        }catch (JsonResponseException e){
            log.error("find refund by out id:{} fail,error:{}",outerRefundId,e.getMessage());
            throw new OPServerException(200,e.getMessage());
        }

        DateTime dt = DateTime.parse(cancelOutRefundInfo.getApplyAt(), DFT);
        OpenClientAfterSale afterSale = new OpenClientAfterSale();
        afterSale.setStatus(OpenClientAfterSaleStatus.RETURN_CLOSED);
        afterSale.setReason(cancelOutRefundInfo.getBuyerNote());
        afterSale.setApplyAt(dt.toDate());

        Response<Boolean> response = refundWriteLogic.cancelRefund(refund,afterSale);
        if(!response.isSuccess()){
            log.error("cancel refund(id:{}) fail,error:{}",refund.getId(),response.getError());
            throw new OPServerException(200,response.getError());
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
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }
}
