package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.OpenPushOrderTask;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
public interface OpenPushOrderTaskReadService {
    /**
     * 根据处理状态查找同步失败的订单
     * @param status
     */
    public Response<List<OpenPushOrderTask>> findByStatus(int status);



    /**
     * 根据处理状态查找同步失败的订单
     * @param id
     */
    Response<OpenPushOrderTask> findById(Long id);
}
