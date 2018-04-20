package com.pousheng.middle.open.api.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/11
 * pousheng-middle
 */
@Data
public class YYEdiRefundConfirmItem implements java.io.Serializable{

    private static final long serialVersionUID = 5772967608879718555L;
    private String itemCode;
    private String warhouseCode;
    private String quantity;
}
