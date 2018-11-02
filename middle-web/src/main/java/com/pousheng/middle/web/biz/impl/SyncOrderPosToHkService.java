package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 正向订单同步恒康生成pos
 *
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.web.biz.impl
 * 2018/11/2 13:30
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SYNC_ORDER_POS_TO_HK)
@Service
@Slf4j
public class SyncOrderPosToHkService implements CompensateBizService {
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (log.isDebugEnabled()) {
            log.debug("START-PROCESSING-SYNC-ORDER-POS-HK,poushengCompensateBiz {}", poushengCompensateBiz);
        }
        if (Objects.isNull(poushengCompensateBiz)) {
            log.error("poushengCompensateBiz is null");
            return;
        }
        if (!Objects.equals(poushengCompensateBiz.getBizType(), PoushengCompensateBizType.SYNC_ORDER_POS_TO_HK.name())) {
            log.error("poushengCompensateBiz type error,id {},currentType {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType());
            return;
        }
        Shipment shipment = shipmentReadLogic.findShipmentById(Long.valueOf(poushengCompensateBiz.getBizId()));
        Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
        if (!response.isSuccess()) {
            log.error("syncShipmentPosToHk shipment (id:{}) is error ", shipment.getId());
            throw new BizException(response.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("END-PROCESSING-SYNC-ORDER-POS-HK,poushengCompensateBiz {}", poushengCompensateBiz);
        }
    }
}
