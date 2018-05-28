package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Response;
import io.terminus.open.client.order.service.OpenClientOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@Service
@Slf4j
public class PoushengMiddleCompensateBizProcessorImpl implements PoushengMiddleCompensateBizProcessor {

    @Autowired
    private PoushengMiddleCompensateBizRegistryCenter poushengMiddleCompensateBizRegistryCenter;




    @Override
    public Response<Boolean> doProcess(PoushengCompensateBizType bizType, PoushengCompensateBiz poushengCompensateBiz) {
        return getService(bizType).doProcess(poushengCompensateBiz);
    }


    private PoushengMiddleCompensateBizService getService(PoushengCompensateBizType bizType) {
        return poushengMiddleCompensateBizRegistryCenter.getBizProcessor(bizType);
    }
}
