package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.OpenPushOrderTask;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
public interface OpenPushOrderTaskWriteService {
    /**
     * 新增 openPushOrderTask
     * @param openPushOrderTask
     * @return
     */
    Response<Long> create(OpenPushOrderTask openPushOrderTask);


    /**
     * 更新openPushOrderTask
     * @param openPushOrderTask
     * @return 是否成功
     */
    Response<Boolean> update(OpenPushOrderTask openPushOrderTask);
}
