package com.pousheng.middle.web.utils.export;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
public interface ExportExecutor {

     void execute(Workbook wb, Sheet sheet);
}
