package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/26
 */
public interface MiddleRefundReadService {

    /**
     * 逆向订单分页
     * @param criteria 逆向订单查询条件
     * @return 逆向订单集合
     */
    Response<Paging<Refund>> paging(MiddleRefundCriteria criteria);


    /**
     * 根据退款单id查询 退款订单关联信息
     * @param refundIds 退款单id集合
     * @return 退款订单关联信息
     */
    Response<List<OrderRefund>> findOrderRefundByRefundIds(List<Long> refundIds);

}
