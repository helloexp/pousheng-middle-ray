package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.open.PsOrderPusher;
import com.pousheng.middle.open.PsOrderReceiver;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhaoxw
 * @date 2018/10/30
 */

@CompensateAnnotation(bizType = PoushengCompensateBizType.OPEN_ORDER_PUSH)
@Service
@Slf4j
public class OpenOrderPushService implements CompensateBizService {

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private PsOrderPusher orderPusher;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("push outer order start  ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("OpenOrderImportService.doProcess context is null");
            throw new BizException("OpenOrderImportService.doProcess context is null");
        }
        OpenFullOrderInfo order = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), OpenFullOrderInfo.class);
        OpenShop openShop = openShopCacher.findById(Long.parseLong(poushengCompensateBiz.getBizId()));
        orderPusher.pushOrders(OpenClientShop.from(openShop), Lists.newArrayList(order));
        log.info("push outer order end  ....,poushengCompensateBiz is {}", poushengCompensateBiz);
    }
}
