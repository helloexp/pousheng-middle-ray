package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public interface PoushengMiddleCompensateBizProcessor {

    /**
     * 业务处理过程
     * @param bizType 业务处理类型
     * @param poushengCompensateBiz 业务处理domain
     * @return
     */
    public Response<Boolean> doProcess(PoushengCompensateBizType bizType, PoushengCompensateBiz poushengCompensateBiz);
}
