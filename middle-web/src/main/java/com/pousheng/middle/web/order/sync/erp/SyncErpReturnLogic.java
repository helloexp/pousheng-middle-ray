package com.pousheng.middle.web.order.sync.erp;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.yjerp.SyncYJErpReturnLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiReturnLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * 1.同步售后单到erp
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/15
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncErpReturnLogic {
    @Autowired
    private SyncRefundLogic syncRefundLogic;
    @Autowired
    private SyncYYEdiReturnLogic syncYYEdiReturnLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Value("${skx.open.shop.id}")
    private Long skxOpenShopId;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncYJErpReturnLogic syncYJErpReturnLogic;
    @Autowired
    private WarehouseClient warehouseClient;

    /**
     * 对于退货退款和换货的售后单需要同步到yyedi，对于仅退款的售后单需要直接同步到恒康
     * @param refund
     * @return
     */
    public Response<Boolean> syncReturn(Refund refund){
        log.info("sync refund start,refund is {}",refund);
        Response<OpenShop> openShopResponse = openShopReadService.findById(refund.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",refund.getShopId(),openShopResponse.getError());
        }
        //店发发货单对应的拒收单不允许同步恒康，不允许同步订单派发中心
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        Shipment shipment =  shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE)==null?"hk":openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        //售后仅退款的售后单直接同步到恒康
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())){
            switch (erpSyncType){
                case "hk":
                    //skx的逆向单要同步skx和yyedi
                    if(Objects.equals(openShop.getId(),skxOpenShopId)){
                        return syncRefundLogic.syncRefundToSkx(refund);
                    }else {
                        return syncRefundLogic.syncRefundToHk(refund);
                    }
                case "yyEdi":
                    return this.syncReturnPos(refund);
                default:
                    return syncRefundLogic.syncRefundToHk(refund);
            }
        }
        //如果是店发的拒收单直接同步恒康不用管渠道
        if (Objects.equals(shipment.getShipWay(),1)
                && Objects.equals(refund.getRefundType(),MiddleRefundType.REJECT_GOODS.value())){
            return this.syncSaleRefuse(refund);

        }
        // 共享仓云聚售后拒收类型单
        if (Objects.equals(shipment.getShipWay(),2) && Objects.equals(refund.getRefundType(),MiddleRefundType.REJECT_GOODS.value())){
            if (syncYJErp(shipment.getShipId())) {
                return syncYJErpReturnLogic.syncRefundToYJErp(refund);
            }
        }
        //售后换货，退货的同步走配置渠道
        switch (erpSyncType){
            case "hk":
                //skx的逆向单要同步skx和yyedi
                if(Objects.equals(openShop.getId(),skxOpenShopId)){
                    return syncRefundLogic.syncRefundToSkx(refund);
                }else {
                    return syncRefundLogic.syncRefundToHk(refund);
                }
            case "yyEdi":
                return syncYYEdiReturnLogic.syncRefundToYYEdi(refund);
            default:
                return syncRefundLogic.syncRefundToHk(refund);
        }
    }

    @NotNull
    private Response<Boolean> syncReturnPos(Refund refund) {
        try{
            log.info("sync refund pos start,refund is {}",refund);
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
            refund.setStatus(targetStatus);
            Response<Boolean> r = syncRefundPosLogic.syncRefundPosToHk(refund);
            if (r.isSuccess()){
                //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                refundWriteLogic.getThirdRefundResult(refund);
                return Response.ok(Boolean.TRUE);
            }else{
                updateRefundSyncFial(refund);
                return Response.fail("sync.refund.pos.failed");
            }
        }catch (Exception e){
            log.error("sync pos failed,caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail("sync.refund.pos.failed");
        }

    }

    private Response<Boolean> syncSaleRefuse(Refund refund){
        try{
            log.info("sync sale refuse start,refund  {}",refund);
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
            refund.setStatus(targetStatus);
            Response<Boolean> r = syncRefundPosLogic.syncSaleRefuseToHK(refund);
            if (r.isSuccess()){
                OrderOperation syncSuccessOrderOperation = getSyncSuccessOperation(refund);
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
                if (!updateStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
                return Response.ok(Boolean.TRUE);
            }else{
                updateRefundSyncFial(refund);
                return Response.fail("sync.sale.refuse.failed");
            }
        }catch (Exception e){
            log.error("sync sale refuse failed,caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail("sync.sale.refuse.failed");
        }
    }

    /**
     * 同步erp退货单取消
     *
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncReturnCancel(Refund refund){
        log.info("cancel refund start,refund is {}",refund);
        Response<OpenShop> openShopResponse = openShopReadService.findById(refund.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",refund.getShopId(),openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE)==null?"hk":openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        //售后换货，退货的同步走配置渠道
        switch (erpSyncType){
            case "hk":
                if(Objects.equals(openShop.getId(),skxOpenShopId)){
                    return syncRefundLogic.syncRefundCancelToSkx(refund);
                }else {
                    return syncRefundLogic.syncRefundCancelToHk(refund);
                }
            case "yyEdi":
                if (Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_REFUND.value())){
                    return Response.ok(Boolean.TRUE);
                }
                return syncYYEdiReturnLogic.syncRefundCancelToYyEdi(refund);
            default:
                return Response.ok(Boolean.TRUE);
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
            case REJECT_GOODS:
                return MiddleOrderEvent.SYNC_SALE_REFUSE_SUCCESS.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new ServiceException("refund.type.invalid");
        }

    }

    private Boolean syncYJErp (Long warehouseId) {
        Response<WarehouseDTO> rW  = warehouseClient.findById(warehouseId);
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        WarehouseDTO warehouse = rW.getResult();
        Map<String, String> extra = warehouse.getExtra();
        if (extra.containsKey(TradeConstants.IS_SHARED_STOCK) && Objects.equals(extra.get(TradeConstants.IS_SHARED_STOCK), "Y")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
