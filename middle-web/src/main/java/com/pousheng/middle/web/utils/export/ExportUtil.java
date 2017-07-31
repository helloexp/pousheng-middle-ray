package com.pousheng.middle.web.utils.export;

import com.google.common.base.Throwables;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
public class ExportUtil {


    public static void export(ExportContext context) {

        export(new DefaultExportExecutor(context));
    }

    public static void export(ExportExecutor executor) {

        try (Workbook wb = new HSSFWorkbook()) {

            Sheet sheet = wb.createSheet();

            executor.execute(wb, sheet);

        } catch (Exception e) {
            log.error("export to execl file fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("export.execl.fail");
        }
    }


    static class DefaultExportExecutor implements ExportExecutor {


        private ExportContext context;


        public DefaultExportExecutor(ExportContext context) {
            this.context = context;
        }

        @Override
        public void execute(Workbook wb, Sheet sheet) {

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
                File file = new File(context.getPath(), StringUtils.isBlank(context.getFilename()) ? (DateTime.now().toString("yyyyMMddHHmmss") + ".xls") : context.getFilename());
                try (FileOutputStream out = new FileOutputStream(file)) {
                    wb.write(out);
                    context.setResultFile(file);
                } catch (IOException e) {
                    throw new RuntimeException("save export result to file fail", e);
                }
            } else {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    wb.write(out);
                    context.setResultByteArray(out.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException("save export result to byte array fail", e);
                }
            }


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
