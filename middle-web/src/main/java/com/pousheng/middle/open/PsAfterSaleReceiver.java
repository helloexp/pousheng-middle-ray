package com.pousheng.middle.open;

import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.aftersale.component.DefaultAfterSaleReceiver;
import io.terminus.open.client.center.job.aftersale.dto.SkuOfRefund;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.enums.OpenClientAfterSaleType;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Created by cp on 7/17/17.
 */
@Component
@Slf4j
public class PsAfterSaleReceiver extends DefaultAfterSaleReceiver {

    private final PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private RefundWriteService refundWriteService;

    @Autowired
    public PsAfterSaleReceiver(PoushengMiddleSpuService middleSpuService) {
        this.middleSpuService = middleSpuService;
    }

    @Override
    protected void fillSkuInfo(Refund refund, SkuOfRefund skuOfRefund) {
        if (!StringUtils.hasText(skuOfRefund.getSkuCode())) {
            return;
        }

        Response<SkuTemplate> findR = middleSpuService.findBySkuCode(skuOfRefund.getSkuCode());
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuOfRefund.getSkuCode(), findR.getError());
            return;
        }
        SkuTemplate skuTemplate = findR.getResult();

        //TODO 塞到refund extra

    }

    @Override
    protected Integer toParanaRefundType(OpenClientAfterSaleType type) {
        switch (type) {
            case IN_SALE_REFUND:
                return MiddleRefundType.ON_SALES_REFUND.value();
            case AFTER_SALE_ONLY_REFUND:
                return MiddleRefundType.AFTER_SALES_REFUND.value();
            case AFTER_SALE:
                return MiddleRefundType.AFTER_SALES_RETURN.value();
            case EXCHANGE:
                return MiddleRefundType.AFTER_SALES_CHANGE.value();
            default:
                return null;
        }
    }

    @Override
    protected Integer toParanaRefundStatus(OpenClientAfterSaleStatus status) {
        //TODO 转成中台对应的status
        return super.toParanaRefundStatus(status);
    }

    protected void updateRefund(Refund refund, OpenClientAfterSale afterSale) {
        if (afterSale.getStatus() != OpenClientAfterSaleStatus.SUCCESS) {
            return;
        }

        Response<Boolean> updateR = refundWriteService.updateStatus(refund.getId(), MiddleRefundStatus.REFUND.getValue());
        if (!updateR.isSuccess()) {
            log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                    refund.getId(), MiddleRefundStatus.REFUND.getValue(), afterSale, updateR.getError());
        }
    }
}
