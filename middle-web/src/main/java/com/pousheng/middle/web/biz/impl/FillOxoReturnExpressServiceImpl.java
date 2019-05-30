package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.warehouse.dto.StockDto;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.vip.extra.service.VipOrderReturnService;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import vipapis.order.OxoOrderReturnApply;
import vipapis.order.OxoOrderbarcodes;
import vipapis.order.OxoReturnOrder;
import vipapis.order.OxoReturnOrderResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author bernie
 * <p>
 * 同步唯品退货物流信息
 **/
@CompensateAnnotation(bizType = PoushengCompensateBizType.SYNC_OXO_RETURN_EXPRESS)
@Service
@Slf4j
public class FillOxoReturnExpressServiceImpl implements CompensateBizService {

    @RpcConsumer
    private VipOrderReturnService vipOrderReturnService;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundWriteService refundWriteService;
    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private ExpressCodeReadService expressCodeReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        try {

            List<MiddleOrderRefundDto> middleOrderRefundDtoList = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
                poushengCompensateBiz.getContext(),
                JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class, MiddleOrderRefundDto.class));

            middleOrderRefundDtoList.forEach(middleOrderRefundDto -> {
                log.info("fill.vip.refundId={}.express start,", middleOrderRefundDto.getRefundId());
                OxoReturnOrder oxoReturnOrder = pullReturnExpress(middleOrderRefundDto.getRefundId(),
                    middleOrderRefundDto.getShopId(),
                    middleOrderRefundDto.getOrderOutId());
                if (Objects.isNull(oxoReturnOrder)) {
                    return;
                }
                Refund refund = refundReadLogic.findRefundById(middleOrderRefundDto.getRefundId());

                if (!checkRefund(refund)) {
                    return;
                }
                //退货退款，售后单的完善
                completeHandle(refund, oxoReturnOrder);
                //派发订单到第三方系统
                refund = refundReadLogic.findRefundById(refund.getId());
                if (checkIsCompleteRefundInfo(refund) && allowHandle(refund, MiddleOrderEvent.SYNC_HK)) {
                    log.info("sync refund extra extra info is {}, id is {}", refund.getExtraJson(),
                        refund.getId());
                    Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                    if (!syncRes.isSuccess()) {
                        log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                    }
                }
                log.info("fill.vip.refundId={}.express end,refundId={}", middleOrderRefundDto.getRefundId());
            });
        } catch (Exception e) {
            log.error("Refunds completeHandle failed,casused by {}", Throwables.getStackTraceAsString(e));
        }
    }

    private boolean checkRefund(Refund refund) {

        if (Objects.isNull(refund)) {
            log.info("fill.vip.refundId={}.express.fail,not refund,refundType={}", refund.getId(),
                refund.getRefundType());
            return Boolean.FALSE;
        }

        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
            log.info("fill.vip.refundId={}.express.fail,refundType={}", refund.getId(),
                refund.getRefundType());
            return Boolean.FALSE;
        }

        if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_HANDLE.getValue()) && !Objects
            .equals(refund.getStatus(), MiddleRefundStatus.RETURN_SYNC_HK_SUCCESS.getValue())) {
            log.info("fill.vip.refundId={}.express.fail,currentStatus={}", refund.getId(),
                refund.getStatus());
            return Boolean.FALSE;
        }
        if (!StringUtils.isEmpty(refund.getShipmentSerialNo())) {

            log.info("fill.vip.refundId={}.express.fail,shipmentSerialNo has exist={}", refund.getId(),
                refund.getShipmentSerialNo());
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private OxoReturnOrder pullReturnExpress(Long refundId, Long shopId, String outOrderId) {

        OxoReturnOrder oxoReturnOrder = null;
        //调用vip获取退货单信息
        Response<OxoReturnOrderResponse> response = vipOrderReturnService.getOxoReturnOrder(shopId,
            Lists.newArrayList(outOrderId));
        if (!response.isSuccess()) {
            log.error("fill.vip.refundId={}.query.oxoReturnExpress.fail,orderId={} msg={}", refundId, outOrderId,
                response.getError());
            return null;
        }

        if (CollectionUtils.isEmpty(response.getResult().getOxo_return_orders())) {
            log.error("fill.vip.refundId={}.query.oxoReturnExpress.no.result,orderId={} msg={}", refundId, outOrderId,
                response.getError());
            return null;
        }
        String shipmentCorpName = null;
        String shipmentSerialNo = null;
        try {
            oxoReturnOrder = response.getResult().getOxo_return_orders().get(0);
            //物流公司名称
            shipmentCorpName = oxoReturnOrder.getBarcodes().get(0).getReturnApply()
                .get(0).getCarrier();

            //物流单号
            shipmentSerialNo = oxoReturnOrder.getBarcodes().get(0).getReturnApply()
                .get(0).getTransportNo();

            if (StringUtils.isEmpty(shipmentSerialNo)) {
                log.error("fill.vip.refundId={}.query.oxoReturnExpress.shipmentSeriaNo.empty", refundId);
                return null;
            }
        } catch (Exception e) {
            log.error("fill.vip.refundId={}.query.oxoReturnExpress.exception,msg={}", refundId,
                Throwables.getStackTraceAsString(e));
            return null;
        }

        return oxoReturnOrder;
    }

    private void completeHandle(Refund refund, OxoReturnOrder oxoReturnOrder) {
        //获取当前数据库中已经存在的售后商品信息

        //获取extra信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);

        Map<String, String> extraMap = refund.getExtra();

        //完善仓库及物流信息 多个物流信息暂不处理

        String shipmentCorpName = oxoReturnOrder.getBarcodes().get(0).getReturnApply().get(0).getCarrier();

        //物流公司名称
        String shipmentSerialNo = oxoReturnOrder.getBarcodes().get(0).getReturnApply().get(0).getTransportNo();
        ExpressCodeCriteria expressCodeCriteria=new ExpressCodeCriteria();
        String shipmentCode=toVipCarrierCode(shipmentCorpName);
        expressCodeCriteria.setVipCode(shipmentCode);
        Response<Paging<ExpressCode>> response=expressCodeReadService.pagingExpressCode(expressCodeCriteria);

        if(!response.isSuccess()||Objects.isNull(response.getResult())){
            throw new ServiceException("not.found.vip.logistic");
        }
        if(Objects.isNull(response.getResult().getData())){
            throw new ServiceException("not.found.vip.logistic");
        }


        //物流单号
        refundExtra.setShipmentCorpName(shipmentCorpName);
        refundExtra.setShipmentSerialNo(shipmentSerialNo);
        refundExtra.setShipmentCorpCode(shipmentCode);

        if (!StringUtils.isEmpty(oxoReturnOrder.getBuyer()) || !StringUtils.isEmpty(oxoReturnOrder.getMobile())
            || !StringUtils.isEmpty(oxoReturnOrder.getTel())) {
            ReceiverInfo receiverInfo = new ReceiverInfo();
            receiverInfo.setReceiveUserName(oxoReturnOrder.getBuyer());
            receiverInfo.setMobile(oxoReturnOrder.getMobile());
            receiverInfo.setPhone(oxoReturnOrder.getTel());
            receiverInfo.setDetail(oxoReturnOrder.getAddress());
            refundExtra.setSenderInfo(receiverInfo);
        }
        //更新售后单信息
        Refund updateRefund = new Refund();
        updateRefund.setId(refund.getId());

        //表明售后单的信息已经全部完善
        if (checkIsCompleteRefundInfo(refund)) {
            //添加处理完成时间
            refundExtra.setHandleDoneAt(new Date());
            extraMap.put(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG, "0");
        }
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        updateRefund.setExtra(extraMap);
        updateRefund.setShipmentSerialNo(shipmentSerialNo);
        updateRefund.setShipmentCorpCode(shipmentCode);
        //寄件人信息
        Response<Boolean> updateRes = refundWriteService.update(updateRefund);
        if (!updateRes.isSuccess()) {
            log.error("fill.vip.refundId={}.update.refund:{} fail,error:{}", updateRefund.getId(), updateRefund,
                updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
        //提交动作
        if (checkIsCompleteRefundInfo(refund) && allowHandle(refund, MiddleOrderEvent.HANDLE)) {
            //更新售后单状态
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund,
                MiddleOrderEvent.HANDLE.toOrderOperation());
            if (!updateStatusRes.isSuccess()) {
                log.error("fill.vip.refundId={}.update status to:{} fail,error:{}", refund.getId(),
                    updateStatusRes.getError());

            }
        }
    }

    private boolean allowHandle(Refund refund, MiddleOrderEvent middleOrderEvent) {

        Flow flow = flowPicker.pickAfterSales();
        return flow.operationAllowed(refund.getStatus(), middleOrderEvent.toOrderOperation());
    }

    private boolean checkIsCompleteRefundInfo(Refund refund) {

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);

        //仓库信息
        if (Objects.isNull(refundExtra.getWarehouseId())) {
            log.info("fill.vip.refundId={} not complete warehouseId is null", refund.getId());
            return Boolean.FALSE;
        }
        //发货单信息
        if (Objects.isNull(refundExtra.getShipmentId())) {
            log.info("fill.vip.refundId={}not complete shipment id is null", refund.getId());
            return Boolean.FALSE;
        }
        //物流信息
        if (Objects.isNull(refundExtra.getShipmentSerialNo())) {
            log.info("fill.vip.refundId={} not complete shipmentSerialNo is null", refund.getId());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private static final ImmutableBiMap<String, String> AFTER_SALE_CARRIER_MAP =
        ImmutableBiMap.<String, String>builder()
            .put("1000000454","中国邮政速递物流")
            .put("1000000455","上海圆通速递")
            .put("1000000455","上海圆通速递")
            .put("1000000456","上海韵达货运")
            .put("1000000457","申通快递")
            .put("1000000458","顺丰速运")
            .put("1000000459","中通速递")
            .put("1000000460","百世汇通")
            .put("1000000461","天天快递")
            .put("1000000462","德邦物流")
            .put("1000000464","天地华宇")
            .put("1000000465","宅急送")
            .put("1000000468","恒路物流")
            .put("1000000474","安能物流")
            .put("1000000475","新邦物流")
            .put("1000000511","中铁物流")
            .put("1200000530","联邦快递")
            .put("1200000570","中远e环球")
            .put("1200000573","UCS")
            .put("1200000640","品骏快递")
            .put("120001438","京东快递").build();
    public String toVipCarrierCode(String carrierName) {
        if(!AFTER_SALE_CARRIER_MAP.inverse().containsKey(carrierName)){
            //没有在列表中时默认品骏
            return "1200000640";
        }
        return AFTER_SALE_CARRIER_MAP.inverse().get(carrierName);
    }
}
