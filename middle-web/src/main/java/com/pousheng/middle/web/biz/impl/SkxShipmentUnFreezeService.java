package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * skx解挂售后发货单失败之后重试机制
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/6/7
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SKX_SHIPMENT_UNFREEZE)
@Service
@Slf4j
public class SkxShipmentUnFreezeService implements CompensateBizService {
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("skx shipment unfreeze start,biz is {}",poushengCompensateBiz);
        if (!Objects.equals(poushengCompensateBiz.getBizType(),PoushengCompensateBizType.SKX_SHIPMENT_UNFREEZE.name())){
            log.error("notify ecp shipment result  failed");
            throw new BizException("biz.type.is.wrong");
        }
        String refundId = poushengCompensateBiz.getBizId();
        Refund refund = refundReadLogic.findRefundById(Long.valueOf(refundId));
        Response<Boolean> response = syncShipmentLogic.syncUnFreezeSkxShipment(refund.getRefundCode());
        if (!response.isSuccess()){
            log.error("skx shipment unfreeze failed,refundCode is {}",refund.getRefundCode());
            throw new BizException(response.getError());
        }
        log.info("skx shipment unfreeze end,biz is {}",poushengCompensateBiz);
    }
}
