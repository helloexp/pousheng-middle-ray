package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.PoushengSettlementPos;
import io.terminus.common.model.Response;


/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */
public interface PoushengSettlementPosWriteService {

    /**
     * 创建 pos单
     * @param poushengSettlementPos
     * @return
     */
    Response<Long> create(PoushengSettlementPos poushengSettlementPos);
}
