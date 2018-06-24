package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.model.PoushengCompensateBiz;

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
