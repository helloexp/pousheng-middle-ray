package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhaoxw
 * @date 2018/10/30
 */

@CompensateAnnotation(bizType = PoushengCompensateBizType.OPEN_ORDER_IMPORT)
@Service
@Slf4j
public class OpenOrderImportService implements CompensateBizService {

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private PsOrderReceiver orderReceiver;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        log.info("import outer order start  ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("OpenOrderImportService.doProcess context is null");
            throw new BizException("OpenOrderImportService.doProcess context is null");
        }
        OpenClientFullOrder order = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), OpenClientFullOrder.class);
        OpenShop openShop = openShopCacher.findById(Long.parseLong(poushengCompensateBiz.getBizId()));
        orderReceiver.receiveOrder(OpenClientShop.from(openShop), Lists.newArrayList(order));
        log.info("import outer order end  ....,poushengCompensateBiz is {}", poushengCompensateBiz);
    }
}
