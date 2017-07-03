package com.pousheng.middle.web.order.component;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/7/1
 */
@Component
@Slf4j
public class RefundWriteLogic {


    @Autowired
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    /**
     * 更新换货商品处理数量
     * 判断是否商品已全部处理，如果是则更新状态为 WAIT_SHIP:待发货
     * @param skuCodeAndQuantity 商品编码及数量
     */
    public void updateSkuHandleNumber(Long refundId,Map<String,Integer> skuCodeAndQuantity){

        Refund refund = refundReadLogic.findRefundById(refundId);
        //换货商品
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);

        //是否全部处理
        Boolean isAllHandle= Boolean.TRUE;
        //更新发货数量
        for (RefundItem refundItem : refundChangeItems) {
            if (skuCodeAndQuantity.containsKey(refundItem.getSkuCode())) {
                refundItem.setAlreadyHandleNumber(refundItem.getAlreadyHandleNumber() + skuCodeAndQuantity.get(refundItem.getSkuCode()));
            }

            //如果存在未处理完成的
            if(!Objects.equals(refundItem.getQuantity(),refundItem.getAlreadyHandleNumber())){
                isAllHandle = Boolean.FALSE;
            }
        }

        Refund update = new Refund();
        update.setId(refundId);
        Map<String,String> extrMap = refund.getExtra();
        extrMap.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonDefaultMapper().toJson(refundChangeItems));
        update.setExtra(extrMap);

        Response<Boolean> updateRes = refundWriteService.update(update);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) fail,error:{}",refund,updateRes.getError());
        }

        if(isAllHandle){
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.CREATE_SHIPMENT.toOrderOperation());
            Response<Boolean> updateStatusRes = refundWriteService.updateStatus(refundId,targetStatus);
            if(!updateStatusRes.isSuccess()){
                log.error("update refund(id:{}) status to:{} fail,error:{}",refund,targetStatus,updateRes.getError());
            }
        }

    }



    public Response<Boolean> updateStatus(Refund refund, OrderOperation orderOperation){

        Flow flow = flowPicker.pickAfterSales();
        if(!flow.operationAllowed(refund.getStatus(),orderOperation)){
            log.error("refund(id:{}) current status:{} not allow operation:{}",refund.getId(),refund.getStatus(),orderOperation.getText());
            return Response.fail("shipment.status.invalid");
        }

        Integer targetStatus = flow.target(refund.getStatus(),orderOperation);
        Response<Boolean> updateRes = refundWriteService.updateStatus(refund.getId(),targetStatus);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) status to:{} fail,error:{}",refund.getId(),updateRes.getError());
            return Response.fail(updateRes.getError());
        }

        return Response.ok();

    }

}
