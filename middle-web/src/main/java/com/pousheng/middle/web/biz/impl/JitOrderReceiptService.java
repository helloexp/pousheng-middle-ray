package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.JitOrderReceiptApi;
import com.pousheng.middle.hksyc.dto.JitOrderReceiptRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * jit 订单回执补发
 * @author tanlongjun
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.JIT_ORDER_RECEIPT)
@Service
@Slf4j
public class JitOrderReceiptService implements CompensateBizService {

    @Autowired
    private JitOrderReceiptApi jitOrderReceiptApi;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        if (null == poushengCompensateBiz) {
            log.warn("doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            throw new BizException("doProcess context is null");
        }
        JitOrderReceiptRequest request = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
            context, JitOrderReceiptRequest.class);
        if (request == null) {
            log.error("order param:{}", context);
            throw new BizException("could not handle the order receipt.");
        }

        Response<List<ShopOrder>> resp =  middleOrderReadService.findByOutIdsAndOutFrom(Lists.newArrayList(request.getOrder_sn()),MiddleChannel.YUNJUJIT.getValue());
        if(!resp.isSuccess() || resp.getResult().isEmpty()){
            log.error("failed to find order by out order Id()",request.getOrder_sn());
            throw new BizException("could not handle the order receipt.");
        }
        YJRespone respone = jitOrderReceiptApi.sendReceipt(request,resp.getResult().get(0).getShopId());
        // 若回执发送失败 则创建补偿任务补发
        if (respone == null
                || (respone != null && 0 != respone.getError())) {
            throw new BizException("failed to send jit order receipt");
        }
    }

}
