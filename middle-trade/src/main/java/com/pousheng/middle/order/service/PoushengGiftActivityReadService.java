package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.PoushengGiftActivityCriteria;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/6/28.
 */
public interface PoushengGiftActivityReadService {
    /**
     * 分页查询:赠品活动
     *
     * @param criteria(查询条件)
     * @return
     */
    Response<Paging<PoushengGiftActivity>> paging(PoushengGiftActivityCriteria criteria);

    /**
     * 单个查询:供查询单个活动
     *
     * @param id
     * @return
     */
    Response<PoushengGiftActivity> findById(Long id);
}
