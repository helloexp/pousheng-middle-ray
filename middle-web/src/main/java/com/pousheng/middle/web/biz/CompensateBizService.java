package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.Exception.BizException;
import io.terminus.common.model.Response;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public interface CompensateBizService {

    /**
     * 业务处理过程
     * @param poushengCompensateBiz 业务处理domain
     * @return
     */
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz);
}
