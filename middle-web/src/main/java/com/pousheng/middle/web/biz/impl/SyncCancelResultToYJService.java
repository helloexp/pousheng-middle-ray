package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.hksyc.component.SycYunJuShipmentOrderApi;
import com.pousheng.middle.hksyc.dto.YJSyncCancelRequest;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        try {
            sycYunJuShipmentOrderApi.doSyncCancelResult(yjSyncCancelRequest);
        }catch (Exception e){
            throw new BizException("sync cancel result fail,caused by {}", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("END-PROCESSING-SYNC_CANCEL_TO_YJ {}", poushengCompensateBiz);
        }
    }
}
