/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: NotifyHkOrderDoneLogic
 * Author:   xiehong
 * Date:     2018/5/30 下午3:43
 * Description: 电商状态为已确认，通知恒康发单收货时间
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.open.component;

import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 〈电商状态为已确认，通知恒康发单收货时间〉
 *
 * @author xiehong
 * @create 2018/5/30 下午3:43
 */
@Slf4j
@Component
public class NotifyHkOrderDoneLogic {

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    /**
     * 创建通知恒康发货单收货时间任务
     *
     * @param shopOrderId
     * @return:
     * @Author:xiehong
     * @Date: 2018/5/30 下午4:37
     */
    public Response<Long> ctreateNotifyHkOrderDoneTask(Long shopOrderId) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.NOTIFY_HK_ORDER_DOWN.toString());
        biz.setContext(String.valueOf(shopOrderId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }

}