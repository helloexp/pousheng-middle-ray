package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.Exception.BizException;
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
public class CompensateBizProcessorImpl implements CompensateBizProcessor {

    @Autowired
    private CompensateBizRegistryCenter compensateBizRegistryCenter;




    @Override
    public void
    doProcess(PoushengCompensateBiz poushengCompensateBiz)  {
         this.getService(poushengCompensateBiz).doProcess(poushengCompensateBiz);
    }


    private CompensateBizService getService(PoushengCompensateBiz poushengCompensateBiz) {
        return compensateBizRegistryCenter.getBizProcessor(poushengCompensateBiz);
    }
}
