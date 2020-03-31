package com.pousheng.middle.web.excel.warehouse;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/6/6
 */
@Data
public class WarehouseTagsExcelBean {
    //仓库外码
    private String outWarehouseCode;
    //账套
    private String companyCode;

    private String tag2;

    private String tag3;
}
