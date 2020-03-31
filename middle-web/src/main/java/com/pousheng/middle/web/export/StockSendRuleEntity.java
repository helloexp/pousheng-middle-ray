package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-19 14:10<br/>
 */
@Data
public class StockSendRuleEntity implements Serializable {
    private static final long serialVersionUID = -7337032727776105498L;

    @ExportTitle("规则ID")
    private Long ruleId;
    @ExportTitle("销售店铺")
    private String shopName;
    @ExportTitle("发货仓账套")
    private String warehouseCompanyId;
    @ExportTitle("发货仓外码")
    private String warehouseOutCode;
    @ExportTitle("发货仓名称")
    private String warehouseName;
    @ExportTitle("发货仓类型")
    private String warehouseType;
    @ExportTitle("店铺状态")
    private String warehouseStatus;
}
