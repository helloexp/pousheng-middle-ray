package com.pousheng.middle.web.utils;

import com.google.common.base.MoreObjects;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.RefundItem;
import io.terminus.common.utils.Joiners;
import io.terminus.parana.order.model.ShipmentItem;

/**
 * @author Xiongmin
 * 2019/3/17
 */
public class SkuCodeUtil {

    private static final String EMPTY_STR = "";

    /**
     * skuOrderId + skuCode + outSkuCode
     * @param refundItem
     * @return
     */
    public static final String getCombineCode(RefundItem refundItem) {
        String skuOrderId = refundItem.getSkuOrderId() != null ? refundItem.getSkuOrderId().toString() : EMPTY_STR;
        String skuCode = MoreObjects.firstNonNull(refundItem.getSkuCode(),  EMPTY_STR);
        String outSkuCode = MoreObjects.firstNonNull(refundItem.getOutSkuCode(), EMPTY_STR);
        return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
    }

    /**
     * skuOrderId + skuCode + outSkuCode
     * @param shipmentItem
     * @return
     */
    public static final String getCombineCode(ShipmentItem shipmentItem) {
        String skuOrderId = shipmentItem.getSkuOrderId() != null ? shipmentItem.getSkuOrderId().toString() : EMPTY_STR;
        String skuCode = MoreObjects.firstNonNull(shipmentItem.getSkuCode(), EMPTY_STR);
        String outSkuCode = MoreObjects.firstNonNull(shipmentItem.getOutSkuCode(), EMPTY_STR);
        return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
    }

    /**
     * skuOrderId + skuCode + outSkuCode
     * @param editSubmitRefundItem
     * @return
     */
    public static final String getCombineCode(EditSubmitRefundItem editSubmitRefundItem) {
        String skuOrderId = editSubmitRefundItem.getSkuOrderId() != null
                ? editSubmitRefundItem.getSkuOrderId().toString() : EMPTY_STR;
        String skuCode = editSubmitRefundItem.getRefundSkuCode();
        String outSkuCode = editSubmitRefundItem.getRefundOutSkuCode();
        return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
    }
}
