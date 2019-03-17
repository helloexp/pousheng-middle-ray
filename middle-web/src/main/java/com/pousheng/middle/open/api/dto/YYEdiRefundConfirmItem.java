package com.pousheng.middle.open.api.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/11
 * pousheng-middle
 */
@Data
public class YYEdiRefundConfirmItem implements java.io.Serializable, Comparable<YYEdiRefundConfirmItem> {

    private static final long serialVersionUID = 5772967608879718555L;
    private String itemCode;
    private String warhouseCode;
    private String quantity;
    private String outSkuCode;

    /**
     * 按照 quantity 降序
     * 考虑到 quantity 可能为 null, 这里特殊处理 null 排在最后
     * @param o
     * @return
     */
    @Override
    public int compareTo(YYEdiRefundConfirmItem o) {
        if (o.getQuantity() == null && this.getQuantity() == null) {
            return 0;
        }
        if (this.getQuantity() == null) {
            return 1;
        }
        if (o.getQuantity() == null) {
            return -1;
        }
        return o.getQuantity().compareTo(this.getQuantity());
    }
}
