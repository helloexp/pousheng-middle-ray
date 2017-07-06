package com.pousheng.middle.web.order.sync.ecp;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    //同步ECP退货完成,仅仅是退货退款是用到
    public Response<Boolean> syncRefundToECP(Refund refund)
    {
        //校验售后订单类型
        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_ECP.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund,orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }
        return Response.ok();
    }
}
