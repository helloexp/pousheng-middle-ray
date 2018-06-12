package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.RefundSource;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 同步电商逆向订单逻辑
 * Created by tony on 2017/7/5.
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncRefundToEcpLogic {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;

    //同步ECP退货完成,仅仅是退货退款是用到
    public Response<Boolean> syncRefundToECP(Refund refund)
    {
        //校验售后订单类型
        //非售后退货不可同步
        if(!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())){
            log.error("current refund(id:{}) refund type:{} not allow sync ecp",refund.getId(),refund.getRefundType());
            return Response.fail("refund.not.allow.sync.ecp");
        }
        //非第三方渠道的售后单不可同步
        RefundSource  refundSource = refundReadLogic.findRefundSource(refund);
        if(!Objects.equals(refundSource,RefundSource.THIRD)){
            log.error("current refund(id:{}) refund source:{} not allow sync ecp",refund.getId(),refundSource.value());
            return Response.fail("refund.not.allow.sync.ecp");
        }

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund,orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 根据售后单所属订单来源，调用超鹏接口同步到对应电商平台

        //同步调用成功后，更新售后单的状态
        Refund newStatusRefund = refundReadLogic.findRefundById(refund.getId());
        OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(newStatusRefund, syncSuccessOrderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),orderOperation.getText(),updateSyncStatusRes.getError());
            return Response.fail(updateSyncStatusRes.getError());
        }

        return Response.ok();
    }
}
