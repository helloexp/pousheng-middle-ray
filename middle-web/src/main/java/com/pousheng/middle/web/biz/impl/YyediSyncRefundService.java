/**
 * Copyright (C), 2012-2018, XXX有限公司
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 〈yyedi回传退货信息业务处理〉
 * <p>
 * Author: xiehong (168479)
 * Date: 2018/5/31 上午11:41
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.YYEDI_SYNC_REFUND_RESULT)
@Service
@Slf4j
public class YyediSyncRefundService implements CompensateBizService {

    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        if (null == poushengCompensateBiz) {
            log.warn("YyediSyncRefundService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("YyediSyncRefundService.doProcess context is null");
            return;
        }
        List<Refund> refunds = JsonMapper.nonEmptyMapper().fromJson(context, JsonMapper.nonEmptyMapper().createCollectionType(List.class, Refund.class));
        if (CollectionUtils.isEmpty(refunds)) {
            log.warn("YyediSyncRefundService.doProcess shipInfos is null");
            return;
        }
        refunds.stream().forEach(a -> {
            //退货单编码
            String refundCode = a.getRefundCode();
            try {
                this.refundBiz(a);

            } catch (Exception e) {
                log.error("YyediSyncRefundService. forEach refunds ({}) is error: {}", refundCode, e.getMessage());
            }

        });


    }


    private void refundBiz(Refund refund) {
        //更新扩展信息
        Map<String,String> extra = refund.getExtra();
        if(MapUtils.isEmpty(extra))
            return;
        Refund update = new Refund();
        update.setId(refund.getId());
        update.setExtra(extra);

        Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
        if (!updateExtraRes.isSuccess()) {
            log.error("update rMatrixRequestHeadefund(refundCode:{}) fail,error:{}", refund.getRefundCode(),updateExtraRes.getError());
        }
        //同步pos单到恒康
        //判断pos单是否需要同步恒康,如果退货仓数量全是0
        String itemInfo = extra.get(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO);

        List<YYEdiRefundConfirmItem> items = JsonMapper.nonEmptyMapper().fromJson(itemInfo, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YYEdiRefundConfirmItem.class));
        if (validateYYConfirmedItems(items)) {
            try {
                Response<Boolean> r = syncRefundPosLogic.syncRefundPosToHk(refund);
                if (!r.isSuccess()) {
                    Map<String, Object> param1 = Maps.newHashMap();
                    param1.put("refundId", refund.getId());
                    autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK, r.getError());
                }
            } catch (Exception e) {
                Map<String, Object> param1 = Maps.newHashMap();
                param1.put("refundId", refund.getId());
                autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_REFUND_POS_TO_HK, e.getMessage());
            }
        }
        //如果是淘宝的退货退款单，会将主动查询更新售后单的状态
        refundWriteLogic.getThirdRefundResult(refund);

    }

    private boolean validateYYConfirmedItems(List<YYEdiRefundConfirmItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        } else {
            int count = 0;
            for (YYEdiRefundConfirmItem item : items) {
                if (Objects.equals(item.getQuantity(), "0")) {
                    count++;
                }
            }
            if (count == items.size()) {
                return false;
            } else {
                return true;
            }
        }
    }
}