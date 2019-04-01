package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.NOTIFY_HK)
@Service
@Slf4j
public class HkNotifyService  implements CompensateBizService {
    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        // TODO: 2018/5/28 将来需要删除
        log.info("============================》");
    }
}
