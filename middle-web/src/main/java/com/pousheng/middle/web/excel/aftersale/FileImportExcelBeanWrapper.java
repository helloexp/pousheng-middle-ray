package com.pousheng.middle.web.excel.aftersale;

import lombok.Data;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/5/29
 */
@Data
public class FileImportExcelBeanWrapper {

    private FileImportExcelBean fileImportExcelBean;

    private boolean hasError;

    private String errorMsg;
}
