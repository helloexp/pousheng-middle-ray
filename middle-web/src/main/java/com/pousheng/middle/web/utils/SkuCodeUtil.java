package com.pousheng.middle.web.utils;

import com.google.common.base.MoreObjects;
import com.pousheng.middle.order.dto.EditSubmitChangeItem;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.RefundItem;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.SkuOrder;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author Xiongmin
 * 2019/3/17
 */
public class SkuCodeUtil {

    private static final String EMPTY_STR = "";

    /**
     * skuOrderId + skuCode + outSkuCode
     *
     * @param refundItem
     * @return
     */
    public static final String getCombineCode(RefundItem refundItem) {
        //String skuOrderId = refundItem.getSkuOrderId() != null ? refundItem.getSkuOrderId().toString() : EMPTY_STR;
        //String skuCode = MoreObjects.firstNonNull(refundItem.getSkuCode(), EMPTY_STR);
        //String outSkuCode = MoreObjects.firstNonNull(refundItem.getOutSkuCode(), EMPTY_STR);
        //return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
        return getCombineCode(refundItem.getSkuOrderId(), refundItem.getOutSkuCode(), refundItem.getSkuCode());
    }

    /**
     * skuOrderId + skuCode + outSkuCode
     *
     * @param shipmentItem
     * @return
     */
    public static final String getCombineCode(ShipmentItem shipmentItem) {
        //String skuOrderId = shipmentItem.getSkuOrderId() != null ? shipmentItem.getSkuOrderId().toString() : EMPTY_STR;
        //String skuCode = MoreObjects.firstNonNull(shipmentItem.getSkuCode(), EMPTY_STR);
        //String outSkuCode = MoreObjects.firstNonNull(shipmentItem.getOutSkuCode(), EMPTY_STR);
        //return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
        return getCombineCode(shipmentItem.getSkuOrderId(), shipmentItem.getOutSkuCode(), shipmentItem.getSkuCode());
    }

    /**
     * skuOrderId + skuCode + outSkuCode
     *
     * @param editSubmitRefundItem
     * @return
     */
    public static final String getCombineCode(EditSubmitRefundItem editSubmitRefundItem) {
        //String skuOrderId = editSubmitRefundItem.getSkuOrderId() != null
        //        ? editSubmitRefundItem.getSkuOrderId().toString() : EMPTY_STR;
        //String skuCode = MoreObjects.firstNonNull(editSubmitRefundItem.getRefundSkuCode(), EMPTY_STR);
        //String outSkuCode = MoreObjects.firstNonNull(editSubmitRefundItem.getRefundOutSkuCode(), EMPTY_STR);
        //return Joiners.COLON.join(skuOrderId, skuCode, outSkuCode);
        return getCombineCode(editSubmitRefundItem.getSkuOrderId(), editSubmitRefundItem.getRefundOutSkuCode(), editSubmitRefundItem.getRefundSkuCode());
    }

    /**
     * 优先按照skuOrderId匹配，若skuOrderId为null，则按照outSkuCode匹配，若outSkuCode为null，则按照skuCode匹配（可能存在问题）
     *
     * @param shipmentItem
     * @param skuOrder
     * @return
     */
    public static final boolean compareCombineCode(ShipmentItem shipmentItem, SkuOrder skuOrder) {
        if (Objects.nonNull(shipmentItem.getSkuOrderId())) {
            return Objects.equals(shipmentItem.getSkuOrderId(), skuOrder.getId());
        }
        if (!StringUtils.isEmpty(shipmentItem.getOutSkuCode()) && !StringUtils.isEmpty(skuOrder.getOutSkuId())) {
            return Objects.equals(shipmentItem.getOutSkuCode(), skuOrder.getOutSkuId());
        }
        return Objects.equals(shipmentItem.getSkuCode(), skuOrder.getSkuCode());
    }

    /**
     * 优先按照skuOrderId匹配，若skuOrderId为null，则按照outSkuCode匹配，若outSkuCode为null，则按照skuCode匹配（可能存在问题）
     *
     * @param shipmentItem
     * @param refundItem
     * @return
     */
    public static final boolean compareCombineCode(ShipmentItem shipmentItem, RefundItem refundItem) {
        if (Objects.nonNull(shipmentItem.getSkuOrderId()) && Objects.nonNull(refundItem.getSkuOrderId())) {
            return Objects.equals(shipmentItem.getSkuOrderId(), refundItem.getSkuOrderId());
        }
        if (!StringUtils.isEmpty(shipmentItem.getOutSkuCode()) && !StringUtils.isEmpty(refundItem.getOutSkuCode())) {
            return Objects.equals(shipmentItem.getOutSkuCode(), refundItem.getOutSkuCode());
        }
        return Objects.equals(shipmentItem.getSkuCode(), refundItem.getSkuCode());
    }

    /**
     * skuOrderId:skuCode
     *
     * @param editSubmitChangeItem
     * @return
     */
    public static String toCombineCode(EditSubmitChangeItem editSubmitChangeItem) {
        String skuOrderId = editSubmitChangeItem.getSkuOrderId() != null ? editSubmitChangeItem.getSkuOrderId().toString() : EMPTY_STR;
        String skuCode = MoreObjects.firstNonNull(editSubmitChangeItem.getChangeSkuCode(), EMPTY_STR);
        return String.format("%s:%s", skuOrderId, skuCode);
    }

    /**
     * skuOrderId:skuCode
     *
     * @param refundItem
     * @return
     */
    public static String toCombineCode(RefundItem refundItem) {
        String skuOrderId = refundItem.getSkuOrderId() != null ? refundItem.getSkuOrderId().toString() : EMPTY_STR;
        String skuCode = MoreObjects.firstNonNull(refundItem.getSkuCode(), EMPTY_STR);
        return String.format("%s:%s", skuOrderId, skuCode);
    }

    public static String getCombineCode(Long skuOrderId, String outSkuCode, String skuCode) {
        if (Objects.nonNull(skuOrderId)) {
            return skuOrderId.toString();
        }
        if (!StringUtils.isEmpty(outSkuCode)) {
            return outSkuCode;
        }
        return skuCode;
    }
}
