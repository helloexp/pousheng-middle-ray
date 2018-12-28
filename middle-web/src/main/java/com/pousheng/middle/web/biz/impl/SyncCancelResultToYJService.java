package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.SycYunJuShipmentOrderApi;
import com.pousheng.middle.hksyc.dto.YJSyncCancelRequest;
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
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 逆向订单同步恒康生成pos
 * @author zxw
 * com.pousheng.middle.web.biz.impl
 * 2018/11/19 16:33
 * pousheng-middle
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SYNC_CANCEL_TO_YJ)
@Service
@Slf4j
public class SyncCancelResultToYJService implements CompensateBizService {

    @Autowired
    private SycYunJuShipmentOrderApi sycYunJuShipmentOrderApi;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (log.isDebugEnabled()) {
            log.debug("START-PROCESSING-SYNC_CANCEL_TO_YJ,poushengCompensateBiz {}", poushengCompensateBiz);
        }
        if (Objects.isNull(poushengCompensateBiz)) {
            log.error("poushengCompensateBiz is null");
            return;
        }
        if (!Objects.equals(poushengCompensateBiz.getBizType(), PoushengCompensateBizType.SYNC_CANCEL_TO_YJ.name())) {
            log.error("poushengCompensateBiz type error,id {},currentType {}", poushengCompensateBiz.getId(), poushengCompensateBiz.getBizType());
            return;
        }
        YJSyncCancelRequest yjSyncCancelRequest = JsonMapper.nonEmptyMapper().fromJson(poushengCompensateBiz.getContext(), YJSyncCancelRequest.class);
        Response<List<ShopOrder>> resp =  middleOrderReadService.findByOutIdsAndOutFrom(Lists.newArrayList(yjSyncCancelRequest.getOrder_sn()),MiddleChannel.YUNJUJIT.getValue());
        if(!resp.isSuccess() || resp.getResult().isEmpty()){
            log.error("failed to find order by out order Id()",yjSyncCancelRequest.getOrder_sn());
            throw new BizException("could not handle the order receipt.");
        }
        try {
            sycYunJuShipmentOrderApi.doSyncCancelResult(yjSyncCancelRequest,resp.getResult().get(0).getShopId());
        }catch (Exception e){
            throw new BizException("sync cancel result fail,caused by {}", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("END-PROCESSING-SYNC_CANCEL_TO_YJ {}", poushengCompensateBiz);
        }
    }
}
