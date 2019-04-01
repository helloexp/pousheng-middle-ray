package com.pousheng.middle.web.utils.export;

import com.google.common.base.Throwables;
import com.pousheng.middle.common.utils.batchhandle.ExportOrder;
import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class ExportClientUtil {
    
    public static void export(ExportContext context,HttpServletRequest request, HttpServletResponse response){
        export(new DefaultExportExecutor(context),request,response);
    }
    
    public static void export(DefaultExportExecutor dfExecutor,HttpServletRequest request,HttpServletResponse response){
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet();

            dfExecutor.execute(wb, sheet,request, response);
            //清除生成的临时文件,关闭连接
            wb.dispose();
        } catch (Exception e) {
            log.error("export to excel file fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("export.excel.fail");
        }
    }
    
    static class DefaultExportExecutor{
        
        private ExportContext context;

        public DefaultExportExecutor(ExportContext context) {
            if (null == context.getData() || context.getData().isEmpty())
                throw new ServiceException("export.data.empty");
            this.context = context;
        }
        
        public void execute(Workbook wb, Sheet sheet, HttpServletRequest request, HttpServletResponse response){

            try {
                setResponseHeader(request, response, context.getFilename());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            List<String> fieldNames = setUpTitleRow(context, sheet.createRow(0));

            int pos = 1;
            for (Object o : context.getData()) {

                Row row = sheet.createRow(pos++);
                if (null != fieldNames && !fieldNames.isEmpty()) {
                    int rowPos = 0;
                    for (String fieldName : fieldNames) {
                        try {
                            Field f = o.getClass().getDeclaredField(fieldName);
                            f.setAccessible(true);
                            Cell cell = row.createCell(rowPos++);
                            Object value = f.get(o);
                            formatterIfNecessary(cell, value, f);
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException("can not find field:" + fieldName + "in " + o.getClass().getName() + " class", e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("cant not access field:" + fieldName + "in " + o.getClass().getName() + " class", e);
                        }

                    }
                }
            }

            if (context.getResultType() == ExportContext.ResultType.FILE) {                
                try {
                    wb.write(response.getOutputStream());
                } catch (IOException e) {
                    throw new RuntimeException("write export result to file fail", e);
                }
            } 
        }
        
        private void setResponseHeader(HttpServletRequest request, HttpServletResponse response, String fileName)
                throws UnsupportedEncodingException {
            final String userAgent = request.getHeader("USER-AGENT");

            String transefileName = StringUtils.isBlank(fileName) ? (DateTime.now().toString("yyyyMMddHHmmss") + ".xlsx") : fileName;
            String finalFileName;
            if (StringUtils.contains(userAgent.toUpperCase(), "MSIE")) {
                //IE浏览器
                finalFileName = URLEncoder.encode(transefileName, "UTF8");
            } else if (StringUtils.contains(userAgent.toUpperCase(), "Mozilla".toUpperCase())) {
                //safari,火狐浏览器
                finalFileName = new String(transefileName.getBytes(), "ISO8859-1");
            } else {
                finalFileName = URLEncoder.encode(transefileName, "UTF8");//其他浏览器, 如chrome等
            }
            response.setHeader("Content-Disposition", "attachment; filename=\"" + finalFileName + "\"");
            response.setContentType("application/octet-stream;charset=utf-8");
        }

        private List<String> setUpTitleRow(ExportContext context, Row row) {
            List<String> fieldNames = new ArrayList<>();
            if (null != context.getTitleContexts() && !context.getTitleContexts().isEmpty()) {
                log.debug("use title context for export title");
                for (int i = 0; i < context.getTitleContexts().size(); i++) {
                    ExportTitleContext titleContext = context.getTitleContexts().get(i);

                    row.createCell(i).setCellValue(titleContext.getTitle());
                    if (null != titleContext.getFieldName())
                        fieldNames.add(titleContext.getFieldName());
                }

            } else {
                log.debug("not specify title context use export entity field annotation for export title");
                //未标注@ExportTitle注解的字段忽略，不导出
                //根据@ExportOrder的值排序。标注@ExportOrder都在未标注的字段前，未标注@ExportOrder的字段按照定声明顺序导出
                Stream.of(context.getData().get(0).getClass().getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(ExportTitle.class))
                        .sorted((f1, f2) -> {
                            if (f1.isAnnotationPresent(ExportOrder.class) && !f2.isAnnotationPresent(ExportOrder.class)) {
                                return -10;
                            } else if (!f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                                return 10;
                            } else if (f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                                return f2.getAnnotation(ExportOrder.class).value() - f1.getAnnotation(ExportOrder.class).value();
                            } else
                                return 0;
                        })
                        .forEach(field -> {
                            fieldNames.add(field.getName());
                            ExportTitle titleAnnotation = field.getAnnotation(ExportTitle.class);
                            row.createCell(row.getPhysicalNumberOfCells()).setCellValue(titleAnnotation.value());
                        });
            }
            return fieldNames;
        }
        
    }

    private static void formatterIfNecessary(Cell cell, Object value, Field field) {
        if (null != value) {
            if (field.isAnnotationPresent(ExportDateFormat.class) && value instanceof Date) {
                String dateFormat = field.getAnnotation(ExportDateFormat.class).value();
                try {
                    cell.setCellValue(DateFormatUtils.format((Date) value, dateFormat));
                    log.debug("use date format [{}] for {}", dateFormat, field.getName());
                } catch (IllegalArgumentException e) {
                    log.warn("the data format [{}] is not effective format,transform to string", dateFormat);
                    cell.setCellValue(value.toString());
                }
            } else
                cell.setCellValue(value.toString());
        }
    }
    
}
