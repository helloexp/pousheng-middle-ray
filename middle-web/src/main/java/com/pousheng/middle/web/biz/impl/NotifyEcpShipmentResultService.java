package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.EcpOrderLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.mpos.SyncMposShipmentLogic;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 通知mpos确认收货
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/6/7
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SYNC_ECP)
@Service
@Slf4j
public class NotifyEcpShipmentResultService implements CompensateBizService {
    @Autowired
    private EcpOrderLogic ecpOrderLogic;
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("notify ecp shipment result start,biz is {}",poushengCompensateBiz);
        if (!Objects.equals(poushengCompensateBiz.getBizType(),PoushengCompensateBizType.SYNC_ECP.name())){
            log.error("notify ecp shipment result  failed");
            throw new BizException("biz.type.is.wrong");
        }
        String shipmentId = poushengCompensateBiz.getBizId();

        ecpOrderLogic.shipToEcp(Long.valueOf(shipmentId));

        log.info("notify ecp shipment result end ,shipmentId {}",poushengCompensateBiz.getBizId());
    }
}
