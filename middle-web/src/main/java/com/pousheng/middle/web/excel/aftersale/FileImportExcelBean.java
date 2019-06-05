package com.pousheng.middle.web.excel.aftersale;

import com.pousheng.middle.common.utils.batchhandle.ExportEditable;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import io.terminus.excel.annotation.ExcelImport;
import io.terminus.excel.annotation.ExcelImportProperty;
import io.terminus.excel.model.ExcelBaseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc   导入excel
 * @Author GuoFeng
 * @Date 2019/5/29
 */
@Data
@ExcelImport(enableSeparator = false)
public class FileImportExcelBean extends ExcelBaseModel {

    @ExcelImportProperty(titleName = "交易单号")
    private String orderNumber;
    /**
     * 售后单类型
     * 1 仅退款
     * 2 退货退款 当前只有2
     * 3 换货
     * 5 丢件补发
     * 6 拒收单
     */
    @ExcelImportProperty(titleName = "售后类型")
    private String type;

    @ExcelImportProperty(titleName = "发货单号")
    private String shipmentOrderNumber;

    @ExcelImportProperty(titleName = "货品条码")
    private String barCode;

    @ExcelImportProperty(titleName = "申请数量")
    private String quantity;

}
