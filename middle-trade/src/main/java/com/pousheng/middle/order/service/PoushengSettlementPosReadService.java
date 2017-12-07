package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */
public interface PoushengSettlementPosReadService {
    /**
     * 分页查询pos单信息
     * @param criteria pos单查询条件
     * @return
     */
    Response<Paging<PoushengSettlementPos>> paging(PoushengSettlementPosCriteria criteria);

    /**
     * 根据pos单号查询pos单信息
     * @param posSerialNo
     * @return
     */
    Response<PoushengSettlementPos> findByPosSerialNo(String posSerialNo);
}
