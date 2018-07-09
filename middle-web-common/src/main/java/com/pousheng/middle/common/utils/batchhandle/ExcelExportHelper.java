package com.pousheng.middle.common.utils.batchhandle;

import com.google.common.collect.Lists;
import io.terminus.common.exception.JsonResponseException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 导出excel辅助类
 *
 * @param <T> penghui
 */
@Slf4j
public class ExcelExportHelper<T> {

    private XSSFWorkbook workbook;

    private Sheet sheet;

    private List<ExportField> exportFields;

    private XSSFCellStyle unLockStyle;

    private ExcelExportHelper(Class clazz) {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet();
        try {
            exportFields = this.extractAttr(clazz, sheet);
        } catch (Exception e) {
            log.error("fail to extract {} attributes", clazz.getName());
            throw new JsonResponseException("fail to create excel");
        }
        unLockStyle = workbook.createCellStyle();
        unLockStyle.setLocked(false);
    }

    public static <T> ExcelExportHelper newExportHelper(Class clazz) {
        return new ExcelExportHelper(clazz);
    }

    /**
     * 追加数据至excel
     *
     * @param t
     */
    public void appendToExcel(T t) {
        int pos = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(pos++);
        if (CollectionUtils.isNotEmpty(exportFields)) {
            int rowPos = 0;
            for (ExportField exportField : exportFields) {
                try {
                    Field f = t.getClass().getDeclaredField(exportField.getName());
                    f.setAccessible(true);
                    Cell cell = row.createCell(rowPos++);
                    Object value = f.get(t);
                    if (Objects.nonNull(value))
                        cell.setCellValue(value.toString());
                    if (sheet.getProtect() && exportField.isCanWrite())
                        cell.setCellStyle(unLockStyle);
                    if (Objects.equals(exportField.getName(), "price")) {
                        String formula = "L" + pos + " * " + "M" + pos + " * " + 0.01;
                        cell.setCellFormula(formula);
                    }
                } catch (NoSuchFieldException e) {
                    log.error("fail find field {} in {} class", exportField.getName(), t.getClass().getName());
                    throw new JsonResponseException("fail append to excel");
                } catch (IllegalAccessException e) {
                    log.error("fail access field {} in {} class", exportField.getName(), t.getClass().getName());
                    throw new JsonResponseException("fail append to excel");
                }
            }
        }
    }

    public void appendToExcel(List<T> list) {
        for (T t : list) {
            this.appendToExcel(t);
        }
    }


    /**
     * 类上标注@ExpoerEditable
     * 未标注@ExportTitle注解的字段忽略，不导出
     * 根据@ExportOrder的值排序。标注@ExportOrder都在未标注的字段前，未标注@ExportOrder的字段按照定声明顺序导出
     *
     * @param clazz
     * @param sheet
     * @return
     */
    private List<ExportField> extractAttr(Class clazz, Sheet sheet) {
        List<ExportField> exportFields = Lists.newArrayList();
        log.debug("not specify title context use export entity field annotation for export title");
        Row row = sheet.createRow(0);
        if (!clazz.isAnnotationPresent(ExportEditable.class)) {
            sheet.protectSheet("terminus");
        }
        Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExportTitle.class))
                .sorted((f1, f2) -> {
                    if (f1.isAnnotationPresent(ExportOrder.class) && !f2.isAnnotationPresent(ExportOrder.class)) {
                        return -1;
                    } else if (!f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                        return 1;
                    } else if (f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                        return f2.getAnnotation(ExportOrder.class).value() - f1.getAnnotation(ExportOrder.class).value();
                    } else
                        return 0;
                })
                .forEach(field -> {
                    ExportField exportField = new ExportField();
                    exportField.setName(field.getName());
                    ExportEditable editableAnnotation = field.getAnnotation(ExportEditable.class);
                    if (editableAnnotation != null)
                        exportField.setCanWrite(editableAnnotation.value());
                    ExportTitle titleAnnotation = field.getAnnotation(ExportTitle.class);
                    exportField.setTitle(titleAnnotation.value());
                    row.createCell(row.getPhysicalNumberOfCells()).setCellValue(titleAnnotation.value());
                    exportFields.add(exportField);
                });
        return exportFields;
    }

    public Workbook getWorkbook() {
        return this.workbook;
    }

    public Integer size() {
        return this.sheet.getLastRowNum();
    }

    /**
     * 转换成文件
     *
     * @return
     */
    public File transformToFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            fileName = DateTime.now().toString("yyyyMMddHHmmss") + ".xls";
        }
        File file = new File(".", fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file);
            this.workbook.write(out);
        } catch (IOException e) {
            log.error("fail transform workbook to file {}", file.getName());
            throw new JsonResponseException("fail save export result to file");
        }
        return file;
    }

    public File transformToFile() {
        return this.transformToFile(null);
    }

    @Data
    class ExportField {

        /**
         * 字段名
         */
        private String name;

        /**
         * 标题
         */
        private String title;

        /**
         * 可写
         */
        private boolean canWrite;

    }
}
