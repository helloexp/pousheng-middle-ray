package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * skx挂起的售后发货单同步失败之后重试机制
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/6/7
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SKX_SHIPMENT_FREEZE)
@Service
@Slf4j
public class SkxShipmentFreezeService implements CompensateBizService {
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("skx shipment freeze start,biz is {}",poushengCompensateBiz);
        if (!Objects.equals(poushengCompensateBiz.getBizType(),PoushengCompensateBizType.SKX_SHIPMENT_FREEZE.name())){
            throw new BizException("biz.type.is.wrong");
        }
        String shipmentId = poushengCompensateBiz.getBizId();
        Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(shipmentId));
        Response<Boolean> response = syncShipmentLogic.syncShipmentToHk(shipment,TradeConstants.SKX_REFUND_FREEZE_FLAG);
        if (!response.getResult()){
            log.error("skx shipment freeze failed,shipmentCode is {}",shipment.getShipmentCode());
            throw new BizException(response.getError());
        }
        log.info("skx shipment freeze end,biz is {}",poushengCompensateBiz);
    }
}
