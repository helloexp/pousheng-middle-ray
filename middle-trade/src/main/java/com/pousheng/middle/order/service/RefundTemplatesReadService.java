package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.Refundtemplates;
import io.terminus.common.model.Response;

import java.util.List;

public interface RefundTemplatesReadService {

    /**
     * 根据批次码查询售后单列表
     * @param batchcode
     * @return
     */
    Response<List<Refundtemplates>> findByBatchCode(String batchcode);
}
