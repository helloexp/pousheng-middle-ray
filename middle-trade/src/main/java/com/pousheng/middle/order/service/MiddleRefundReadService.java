package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.RefundPaging;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;

/**
 * Created by songrenfei on 2017/6/26
 */
public interface MiddleRefundReadService {

    /**
     * 逆向订单分页
     * @param criteria
     * @return
     */
    Response<Paging<RefundPaging>> paging(RefundCriteria criteria);

}
