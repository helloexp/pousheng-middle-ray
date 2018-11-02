package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 逆向订单同步恒康生成pos
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.web.biz.impl
 * 2018/11/2 13:31
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SYNC_AFTERSALE_POS_TO_HK)
@Service
@Slf4j
public class SyncAfterSalePosToHkService implements CompensateBizService {
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (log.isDebugEnabled()) {
            log.debug("START-PROCESSING-SYNC-AFTERSALE-POS-HK,poushengCompensateBiz {}", poushengCompensateBiz);
        }
        if (Objects.isNull(poushengCompensateBiz)) {
            log.error("poushengCompensateBiz is null");
            return;
        }
        if (!Objects.equals(poushengCompensateBiz.getBizType(), PoushengCompensateBizType.SYNC_AFTERSALE_POS_TO_HK.name())) {
            log.error("poushengCompensateBiz type error,id {},currentType {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType());
            return;
        }
        Refund refund = refundReadLogic.findRefundById(Long.valueOf(poushengCompensateBiz.getBizId()));
        Response<Boolean> r = syncRefundPosLogic.syncRefundPosToHk(refund);
        if (!r.isSuccess()) {
            log.error("syncShipmentPosToHk refund (id:{}) is error ", refund.getId());
            throw new BizException(r.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("END-PROCESSING-SYNC-AFTESALE-POS-HK,poushengCompensateBiz {}", poushengCompensateBiz);
        }
    }
}
