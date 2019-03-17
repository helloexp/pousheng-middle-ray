package com.pousheng.middle.web.utils;

import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.dto.RefundItem;
import io.terminus.parana.order.model.ShipmentItem;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Xiongmin
 * 2019/3/17
 */
public class OutSkuCodeUtil {

    public static final String getRefundItemOutSkuCode(RefundItem refundItem) {
        if (StringUtils.isNotEmpty(refundItem.getOutSkuCode())) {
            return refundItem.getOutSkuCode();
        }
        return refundItem.getSkuCode();
    }

    public static final String getShipmentItemOutSkuCode(ShipmentItem shipmentItem) {
        if (StringUtils.isNotEmpty(shipmentItem.getOutSkuCode())) {
            return shipmentItem.getOutSkuCode();
        }
        return shipmentItem.getSkuCode();
    }

    public static final String getYYEdiRefundConfirmItemOutSkuCode(YYEdiRefundConfirmItem yyEdiRefundConfirmItem) {
        if (StringUtils.isNotEmpty(yyEdiRefundConfirmItem.getOutSkuCode())) {
            return yyEdiRefundConfirmItem.getOutSkuCode();
        }
        return yyEdiRefundConfirmItem.getItemCode();
    }
}
