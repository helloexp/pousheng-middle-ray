package com.pousheng.middle.web.export;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
public class ExportUtil {


    public static File export(ExportContext context) {

        if (null == context)
            return null;
        if (null == context.getData() || context.getData().isEmpty())
            return null;

        return export(new DefaultExportExecutor(context));
    }

    public static void export(ExportContext context, OutputStream out) {
        if (null == context)
            return;
        if (null == context.getData() || context.getData().isEmpty())
            return;

        export(new DefaultExportExecutor(context, out));
    }


    public static File export(ExportExecutor executor) {


        try (Workbook wb = new HSSFWorkbook()) {

            Sheet sheet = wb.createSheet();

            return executor.execute(wb, sheet);

        } catch (IOException e) {
            return null;
        }
    }


    static class DefaultExportExecutor implements ExportExecutor {


        private ExportContext context;

        private OutputStream out;


        public DefaultExportExecutor(ExportContext context) {
            this.context = context;
        }


        public DefaultExportExecutor(ExportContext context, OutputStream out) {
            this.context = context;
            this.out = out;
        }

        @Override
        public File execute(Workbook wb, Sheet sheet) {

            List<String> fieldNames = setUpTitleRow(context, sheet.createRow(0));

            int pos = 1;
            for (Object o : context.getData()) {

                Row row = sheet.createRow(pos);
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

                        } catch (IllegalAccessException e) {

                        }

                    }
                }
            }


            if (null == out) {
                File file = new File(System.currentTimeMillis() + ".xls");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    wb.write(out);
                    return file;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    wb.write(out);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


        }

        private List<String> setUpTitleRow(ExportContext context, Row row) {


            List<String> fieldNames = new ArrayList<>();
            if (null != context.getTitleContexts() && !context.getTitleContexts().isEmpty()) {

                for (int i = 0; i < context.getTitleContexts().size(); i++) {
                    ExportTitleContext titleContext = context.getTitleContexts().get(i);

                    row.createCell(i).setCellValue(titleContext.getTitle());
                    if (null != titleContext.getFieldName())
                        fieldNames.add(titleContext.getFieldName());
                }

            } else {

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
                try {
                    cell.setCellValue(DateFormatUtils.format((Date) value, field.getAnnotation(ExportDateFormat.class).value()));
                } catch (IllegalArgumentException e) {
                    cell.setCellValue(value.toString());
                }
            } else
                cell.setCellValue(value.toString());
        }
    }

}
