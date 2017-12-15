package com.pousheng.middle.web.item.batchhandle;

import com.pousheng.middle.web.export.SearchSkuTemplateEntity;
import com.pousheng.middle.web.utils.export.ExportEditable;
import com.pousheng.middle.web.utils.export.ExportOrder;
import com.pousheng.middle.web.utils.export.ExportTitle;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.util.Lists;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 导出excel辅助类
 * @param <T>
 */
@Slf4j
public class ExcelExportHelper<T> {

    /**
     * 1.先读取class 获取每个字段的字段名，标题，是否可写
     * 2.传入list，追加至excel
     */

    private XSSFWorkbook workbook;

    private Sheet sheet;

    private List<ExportField> exportFields;

    private XSSFCellStyle unLockStyle;

    private ExcelExportHelper(Class clazz){
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet();
        exportFields = this.extractAttr(clazz,sheet);
        unLockStyle =  workbook.createCellStyle();
        unLockStyle.setLocked(false);
    }

    public static <T> ExcelExportHelper newExportHelper(Class clazz){
        return new ExcelExportHelper(clazz);
    }

    public boolean appendToExcel(T t){
       return this.appendToExcel(Lists.newArrayList(t));
    }

    public boolean appendToExcel(List<T> list){
        int pos = sheet.getLastRowNum() + 1;
        for (T t:list) {
            Row row = sheet.createRow(pos ++ );
            if(CollectionUtils.isNotEmpty(exportFields)) {
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
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("can not find field:" + exportField.getName() + "in " + t.getClass().getName() + " class", e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("cant not access field:" + exportField.getName() + "in " + t.getClass().getName() + " class", e);
                    }
                }
            }
        }
        return true;
    }


    /**
     * 类上标注@ExpoerEditable
     * 未标注@ExportTitle注解的字段忽略，不导出
     * 根据@ExportOrder的值排序。标注@ExportOrder都在未标注的字段前，未标注@ExportOrder的字段按照定声明顺序导出
     * @param clazz
     * @param sheet
     * @return
     */
    private List<ExportField> extractAttr(Class clazz, Sheet sheet) {
        List<ExportField> exportFields = Lists.newArrayList();
        log.debug("not specify title context use export entity field annotation for export title");
        Row row = sheet.createRow(0);
        if(!clazz.isAnnotationPresent(ExportEditable.class)){
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
                    exportField.setCanWrite(editableAnnotation.value());
                    ExportTitle titleAnnotation = field.getAnnotation(ExportTitle.class);
                    exportField.setTitle(titleAnnotation.value());
                    row.createCell(row.getPhysicalNumberOfCells()).setCellValue(titleAnnotation.value());
                });
        return exportFields;
    }

    public Workbook getWorkbook(){
        return this.workbook;
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

    public static void main(String[] args) {
        Field[] fields = SearchSkuTemplateEntity.class.getDeclaredFields();
        for (Field field:fields) {
            System.out.println(field.getName());
        }
    }
}
