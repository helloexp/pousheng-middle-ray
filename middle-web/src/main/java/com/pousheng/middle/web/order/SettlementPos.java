package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
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
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if(log.isDebugEnabled()){
            log.debug("API-SETTLEMENT-POS-PAGING-START param: criteria [{}] ",criteriaStr);
        }
        Response<Paging<PoushengSettlementPos>> r =  poushengSettlementPosReadService.paging(criteria);
        if (!r.isSuccess()){
            log.error("failed to paging settlement pos, criteria={}, cause:{}",criteria, r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<PoushengSettlementPos> poushengSettlementPosList = r.getResult().getData();
        if (poushengSettlementPosList.size()>0){
            poushengSettlementPosList.forEach(poushengSettlementPos -> {
                if (Objects.equals(poushengSettlementPos.getShipType(),1)){
                    ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(poushengSettlementPos.getOrderId());
                    poushengSettlementPos.setStatus(shopOrder.getStatus());
                }else{
                    Refund refund = refundReadLogic.findRefundByRefundCode(poushengSettlementPos.getOrderId());
                    poushengSettlementPos.setStatus(refund.getStatus());
                }
            });
        }
        r.getResult().setData(poushengSettlementPosList);
        if(log.isDebugEnabled()){
            log.debug("API-SETTLEMENT-POS-PAGING-END param: criteria [{}] ,resp: [{}]",criteriaStr,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }
}
