package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengGiftActivity;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/6/28.
 *
 */
public interface PoushengGiftActivityWriteService {

    /**
     * 该方法可用于创建赠品活动
     *
     * @param poushengGiftActivity,
     * @return id, 返回快递机构的id
     */
    public Response<Long> create(PoushengGiftActivity poushengGiftActivity);


    /**
     * 用于更新赠品活动信息
     *
     * @param poushengGiftActivity 活动信息
     * @return
     */
    public Response<Boolean> update(PoushengGiftActivity poushengGiftActivity);


    /**
     * 用于删除活动
     *
     * @param id,活动主键
     * @return
     */
    public Response<Boolean> deleteById(Long id);


}
