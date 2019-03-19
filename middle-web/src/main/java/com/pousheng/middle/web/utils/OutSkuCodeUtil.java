package com.pousheng.middle.web.utils;

import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.RefundItem;
import io.terminus.parana.order.model.ShipmentItem;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Xiongmin
 * 2019/3/17
 */
public class OutSkuCodeUtil {

    public static final String getRefundItemComplexSkuCode(RefundItem refundItem) {
        if (StringUtils.isEmpty(refundItem.getOutSkuCode())) {
            return refundItem.getSkuCode();
        }
        return refundItem.getSkuCode() + refundItem.getOutSkuCode();
    }

    public static final String getShipmentItemComplexSkuCode(ShipmentItem shipmentItem) {
        if (StringUtils.isEmpty(shipmentItem.getOutSkuCode())) {
            return shipmentItem.getSkuCode();
        }
        return shipmentItem.getSkuCode() + shipmentItem.getOutSkuCode();
    }

    public static final String getYYEdiRefundConfirmItemComplexSkuCode(YYEdiRefundConfirmItem yyEdiRefundConfirmItem) {
        if (StringUtils.isEmpty(yyEdiRefundConfirmItem.getOutSkuCode())) {
            return yyEdiRefundConfirmItem.getItemCode();
        }
        return yyEdiRefundConfirmItem.getItemCode() + yyEdiRefundConfirmItem.getOutSkuCode();
    }

    public static final String getEditSubmitRefundItemComplexSkuCode(EditSubmitRefundItem editSubmitRefundItem) {
        if (StringUtils.isEmpty(editSubmitRefundItem.getRefundOutSkuCode())) {
            return editSubmitRefundItem.getRefundSkuCode();
        }
        return editSubmitRefundItem.getRefundSkuCode() + editSubmitRefundItem.getRefundOutSkuCode();
    }
}
