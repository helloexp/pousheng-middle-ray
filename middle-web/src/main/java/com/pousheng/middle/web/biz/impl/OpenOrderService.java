package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.open.component.OpenOrderConverter;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.job.order.component.OrderExecutor;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 异步创建订单任务
 * @author: yjc
 * @date: 2018/8/28下午2:26
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.OUT_OPEN_ORDER)
@Service
@Slf4j
public class OpenOrderService implements CompensateBizService {

    @Autowired
    private OpenOrderConverter openOrderConverter;
    @Autowired
    private OrderExecutor orderExecutor;
    @Autowired
    private OpenShopCacher openShopCacher;
    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("OpenOrderService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("OpenOrderService.doProcess context is null");
            throw new BizException("OpenOrderService.doProcess context is null");
        }
        try {
            OpenFullOrderInfo openFullOrderInfo = JsonMapper.nonEmptyMapper().fromJson(context, OpenFullOrderInfo.class);
            String shopCode = openFullOrderInfo.getOrder().getCompanyCode()+"-"+openFullOrderInfo.getOrder().getShopCode();
            //查询店铺的信息，如果没有就新建一个
            Response<List<OpenClientShop>> rP = openShopReadService.search(null,null,shopCode);
            if (!rP.isSuccess()){
                log.error("find open shop failed,shopCode is {},caused by {}",shopCode,rP.getError());
                throw new ServiceException("find.open.shop.failed");
            }
            List<OpenClientShop> openClientShops = rP.getResult();
            java.util.Optional<OpenClientShop> openClientShopOptional =  openClientShops.stream().findAny();
            OpenClientShop openClientShop =   openClientShopOptional.get();
            Long openShopId = openClientShop.getOpenShopId();
            OpenShop openShop = openShopCacher.findById(openShopId);
            //组装参数
            OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo);
            orderExecutor.importOrder(openShop, Lists.newArrayList(openClientFullOrder));
        } catch (Exception e) {
            // 订单插入失败需要调用云聚接口进行落地回执
            // TODO
        }
    }
}

