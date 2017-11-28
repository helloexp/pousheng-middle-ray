package com.pousheng.middle.web.order;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */
@RestController
@Slf4j
public class SettlementPos {
    @RpcConsumer
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @RpcConsumer
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private RefundReadLogic refundReadLogic;

    @RequestMapping(value = "/api/settlement/pos/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<PoushengSettlementPos> findBy(PoushengSettlementPosCriteria criteria){
        Response<Paging<PoushengSettlementPos>> r =  poushengSettlementPosReadService.paging(criteria);
        if (!r.isSuccess()){
            log.error("failed to paging settlement pos, criteria={}, cause:{}",criteria, r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<PoushengSettlementPos> poushengSettlementPosList = r.getResult().getData();
        if (poushengSettlementPosList.size()>0){
            poushengSettlementPosList.forEach(poushengSettlementPos -> {
                if (Objects.equals(poushengSettlementPos.getShipType(),1)){
                    ShopOrder shopOrder = orderReadLogic.findShopOrderById(poushengSettlementPos.getOrderId());
                    poushengSettlementPos.setStatus(shopOrder.getStatus());
                }else{
                    Refund refund = refundReadLogic.findRefundById(poushengSettlementPos.getOrderId());
                    poushengSettlementPos.setStatus(refund.getStatus());
                }
            });
        }
        r.getResult().setData(poushengSettlementPosList);
        return r.getResult();
    }
}
