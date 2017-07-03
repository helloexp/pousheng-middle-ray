package com.pousheng.middle.web.order.sync.hk;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 同步恒康逆向订单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundLogic {

    @Autowired
    private RefundWriteLogic refundWriteLogic;

    /**
     * 同步恒康退货单
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundToHk(Refund refund){

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.SYNC_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }
        //要根据不同步的售后单类型来决定同步成功或失败的状态

        //todo 同步恒康，同步调用成功后，更新售后单的状态，及冗余恒康售后单号

        return Response.ok();

    }


    /**
     * 同步恒康退货单取消
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncRefundCancelToHk(Refund refund){

        //更新状态为同步中
        OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),orderOperation.getText(),updateStatusRes.getError());
            return Response.fail(updateStatusRes.getError());
        }

        //todo 同步恒康，同步调用成功后，更新售后单的状态

        return Response.ok(Boolean.TRUE);

    }
    
}
