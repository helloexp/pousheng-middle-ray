package com.pousheng.middle.web.utils.export;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/9/3
 */
@Slf4j
public class ExportLoadTemplateUtil {

    public static void exportTemplateExcel(String fileName, String title, List<String> headers, HttpServletRequest request, HttpServletResponse response) {
        try {
            setResponseHeader(fileName, request, response);
        } catch (UnsupportedEncodingException e1) {
            log.error("loadTemplateExcel setResponseHeader failed,fileName:{}, cause:{}",fileName, Throwables.getStackTraceAsString(e1));
        }
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(title);
        Row row = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle(); // 样式对象
        style.setVerticalAlignment(VerticalAlignment.CENTER);// 垂直
        style.setAlignment(HorizontalAlignment.CENTER);// 水平
        createCell(headers, style, row, sheet);
        try {
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e2) {
            log.error("loadTemplateExcel failed,fileName:{}, cause:{}",fileName, Throwables.getStackTraceAsString(e2));
        }
    }

    private static void createCell(List<String> list, CellStyle style, Row row, Sheet sheet) {
        for (int j = 0; j < list.size(); j++) {
            sheet.setDefaultColumnStyle(j, style);
            sheet.setColumnWidth(j, list.get(j).getBytes().length * 256);
            Cell cell = row.createCell(j);
            RichTextString text = new XSSFRichTextString(list.get(j));
            cell.setCellValue(text);
        }
    }

    private static void setResponseHeader(String fileName, HttpServletRequest request, HttpServletResponse response)
            throws UnsupportedEncodingException {
        final String userAgent = request.getHeader("USER-AGENT");
        String finalFileName;
        if (org.apache.commons.lang3.StringUtils.contains(userAgent.toUpperCase(), "MSIE")) {
            //IE浏览器
            finalFileName = URLEncoder.encode(fileName, "UTF8");
        } else if (org.apache.commons.lang3.StringUtils.contains(userAgent.toUpperCase(), "Mozilla".toUpperCase())) {
            //safari,火狐浏览器
            finalFileName = new String(fileName.getBytes(), "ISO8859-1");
        } else {
            finalFileName = URLEncoder.encode(fileName, "UTF8");//其他浏览器, 如chrome等
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + finalFileName + "\"");
        response.setContentType("application/octet-stream;charset=utf-8");
    }
}
