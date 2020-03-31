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

    @ExportTitle("外部交易单号")
    private String outOrderNumber;

    @ExportTitle("电商sku")
    private String outSkuCode;
    /**
     * 售后单类型
     * 1 仅退款
     * 2 退货退款 当前只有2
     * 3 换货
     * 5 丢件补发
     * 6 拒收单
     */
    @ExportTitle("售后类型")
    private String type;

    @ExportTitle("申请数量")
    private String quantity;

    @ExportTitle("退回物流公司")
    private String logisticsCompany;

    @ExportTitle("退回运单号")
    private String trackingNumber;

    @ExportTitle("导入失败原因")
    private String faildReason;
}
