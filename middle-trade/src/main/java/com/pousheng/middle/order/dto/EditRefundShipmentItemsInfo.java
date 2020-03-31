package com.pousheng.middle.order.dto;

import lombok.Data;
import java.util.List;
/**
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.order.dto
 * 2018/11/13 11:52
 * pousheng-middle
 */
@Data
public class EditRefundShipmentItemsInfo {
    /**
     * 售后单id
     */
    private Long refundId;

    /**
     * 换货列表，这边只关心skuCode(货品条码),applyQuantity(申请数量);
     */
    private List<RefundItem>  changeItems;
}
