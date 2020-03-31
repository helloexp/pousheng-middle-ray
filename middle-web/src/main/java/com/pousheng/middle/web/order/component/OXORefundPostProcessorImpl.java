package com.pousheng.middle.web.order.component;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author bernie
 * @date 2019/6/17
 * @desc 唯品会售后退款订单自动派发
 */
@Component
@Slf4j
public class OXORefundPostProcessorImpl implements RefundPostProcessor {

    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;

    @Override
    public void postProcessorAfterCreated(Long refundId) {

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            Refund refund = refundReadLogic.findRefundById(refundId);
            if (Objects.isNull(refund)) {
                log.debug("not.fund.refund.by.refundId={}", refundId);
                return;
            }
            if (!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())) {
                return;
            }
            if (checkIsCompleteRefundInfo(refund)) {
                //更新售后单状态
                Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund,
                    MiddleOrderEvent.HANDLE.toOrderOperation());
                if (!updateStatusRes.isSuccess()) {
                    log.error("fill.vip.refundId={}.update status to:{} fail,error:{}", refund.getId(),
                        updateStatusRes.getError());
                    return;
                }
                //进行自动投递
                refund = refundReadLogic.findRefundById(refundId);
                if (allowHandle(refund, MiddleOrderEvent.SYNC_HK)) {
                    Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                    if (!syncRes.isSuccess()) {
                        log.error("sync refund(id:{}) to third erp  fail,error:{}", refund.getId(), syncRes.getError());
                    }
                }
            }
            stopwatch.stop();
            log.info("receive after order post process end refund id: {} , cost {} ms", refundId, stopwatch.elapsed(
                TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.info("receive after order post process error refundId:{},msg={}", refundId,
                Throwables.getStackTraceAsString(e));
        }

    }

    private boolean allowHandle(Refund refund, MiddleOrderEvent middleOrderEvent) {

        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(refund.getStatus(), middleOrderEvent.toOrderOperation())) {
            log.debug("not.allow.operation refundId={} currentStatus={},expectOperation={}", refund.getId(),
                refund.getStatus(), middleOrderEvent.getText());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private boolean checkIsCompleteRefundInfo(Refund refund) {

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);

        //仓库信息
        if (Objects.isNull(refundExtra.getWarehouseId())) {
            log.info("check.vip.refund.refundId={} not complete warehouseId is null", refund.getId());
            return Boolean.FALSE;
        }
        //发货单信息
        if (Objects.isNull(refundExtra.getShipmentId())) {
            log.info("check.vip.refund.refundId={}not complete shipment id is null", refund.getId());
            return Boolean.FALSE;
        }
        //物流信息
        if (Objects.isNull(refundExtra.getShipmentSerialNo())) {
            log.info("check.vip.refund.refundId={} not complete shipmentSerialNo is null", refund.getId());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
