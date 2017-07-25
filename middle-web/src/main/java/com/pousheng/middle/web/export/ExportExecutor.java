package com.pousheng.middle.web.export;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
public interface ExportExecutor {

     File execute(Workbook wb, Sheet sheet);
}
