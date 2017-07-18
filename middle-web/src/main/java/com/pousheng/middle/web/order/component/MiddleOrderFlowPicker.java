package com.pousheng.middle.web.order.component;

import com.pousheng.middle.order.dto.fsm.MiddleFlowBook;
import io.terminus.parana.order.dto.fsm.Flow;
import org.springframework.stereotype.Component;

/**
 * Created by songrenfei on 2017/6/20
 */
@Component
public class MiddleOrderFlowPicker {

    /**
     * 获取订单流程
     * @return flow
     */
    public Flow pickOrder() {
        return MiddleFlowBook.orderFlow;
    }

    /**
     * 获取发货单流程
     * @return flow
     */
    public Flow pickShipments() {
        return MiddleFlowBook.shipmentsFlow;
    }

    /**
     * 获取售后单流程
     * @return flow
     */
    public Flow pickAfterSales() {
        return MiddleFlowBook.afterSalesFlow;
    }

    /**
     * 获取订单通知电商流程
     * @return
     */
    public Flow pickEcpOrder(){ return MiddleFlowBook.ecpOrderFlow;}
}
