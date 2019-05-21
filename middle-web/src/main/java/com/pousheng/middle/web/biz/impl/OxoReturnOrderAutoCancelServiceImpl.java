package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author bernie 同步唯品退货物流信息
 **/
@CompensateAnnotation(bizType = PoushengCompensateBizType.OXO_REFUND_AUTO_CLOSE)
@Service
@Slf4j
public class OxoReturnOrderAutoCancelServiceImpl implements CompensateBizService {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        log.info("oxo.timeout.auto.cance.refund.id={}",poushengCompensateBiz.getBizId());
        Refund refund = refundReadLogic.findRefundById(Long.valueOf(poushengCompensateBiz.getBizId()));

        if(checkRefundInfo(refund)){
            return;
        }
        Response<Boolean> cancelRes = refundWriteLogic.updateStatusLocking(refund,
            MiddleOrderEvent.CANCEL.toOrderOperation());
        if (!cancelRes.isSuccess()) {
            log.error("oxo.timeout.auto.cancel.refund.id.={} fail,error:{}", poushengCompensateBiz.getBizId(),
                cancelRes.getError());
        }

    }

    private  Boolean checkRefundInfo(Refund refund){

        if(Objects.isNull(refund)){
            return Boolean.FALSE;
        }
        if(!Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_RETURN.value())){
            return Boolean.FALSE;
        }
        if(!Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_HANDLE.getValue())){
            return Boolean.FALSE;
        }
        if (!Objects.equals(refund.getChannel(), MiddleChannel.VIPOXO.getValue())){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
