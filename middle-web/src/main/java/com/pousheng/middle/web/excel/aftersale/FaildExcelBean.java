package com.pousheng.middle.web.excel.aftersale;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc   导入失败excel
 * @Author GuoFeng
 * @Date 2019/5/29
 */
@AllArgsConstructor
@NoArgsConstructor
@ExportEditable(true)
@Data
public class FaildExcelBean {

    @ExportTitle("交易单号")
    private String orderNumber;
    /**
     * 售后单类型
     * 1 仅退款
     * 2 退货退款 当前只有2
     * 3 换货
     * 5 丢件补发
     * 6 拒收单
     */
    @ExportTitle("售后类型")
    private Integer type;

    @ExportTitle("发货单号")
    private String shipmentOrderNumber;

    @ExportTitle("货品条码")
    private String barCode;

    @ExportTitle("申请数量")
    private Integer quantity;

    @ExportTitle("导入失败原因")
    private String faildReason;
}
