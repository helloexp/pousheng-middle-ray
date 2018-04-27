package com.pousheng.middle.web.utils;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.pousheng.middle.order.dto.MiddleOrderInfo;
import com.pousheng.middle.web.utils.export.ExcelCovertCsvReader;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * 导入导出文件工具
 *
 * @param <T>
 */
@Slf4j
public class HandlerFileUtil<T> {

    public static HandlerFileUtil getInstance() {
        return new HandlerFileUtil();
    }

    public List<T> handlerCsv(String file, Class<? extends T> t) {
        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(file + ".csv"), Charset.forName("UTF-8"));
            return handlerCsv(in, t);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<T> handlerCsv(InputStreamReader insr, Class<? extends T> t) {
        CsvToBean csvToBean = new CsvToBeanBuilder<T>(insr).withSkipLines(1).withType(t).build();
        List<T> demoDatas = csvToBean.parse();
        if (!isEmpty(demoDatas)) {
            return demoDatas;
        } else {
            throw new JsonResponseException("file.is.empty");
        }


    }


    /**
     * 限制excel导入最大条数
     */
    private static final Integer MAX_SIZE = 2000;

    public List<MiddleOrderInfo> handlerExcelOrder(InputStream insr) throws IOException {
        List<MiddleOrderInfo> orderInfos = Lists.newArrayList();
        long startTime = System.currentTimeMillis();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(insr);
        try {
            //存在serialVersionUID，需要去掉
            Field[] fields = MiddleOrderInfo.class.getDeclaredFields();
            List<String[]> list = ExcelCovertCsvReader
                    .readerExcelAt(
                            bufferedInputStream, fields.length - 1, MAX_SIZE);
            for (Integer i = 1; i < list.size(); i++) {
                //判断空行
                if (checkDataEmpty(list.get(i))) {
                    continue;
                }
                MiddleOrderInfo middleOrderInfo = makeMiddleOrderInfo(list.get(i), fields);
                if (Strings.isNullOrEmpty(middleOrderInfo.getOutOrderId())) {
                    throw new ServiceException("out.order.id.not.null");
                }
                if (Strings.isNullOrEmpty(middleOrderInfo.getChannel())) {
                    throw new ServiceException("order.channel.not.null");
                }
                checkNotNull(middleOrderInfo.getShipFee(), "order.shipFee.not.null");
                checkNotNull(middleOrderInfo.getFee(), "order.fee.not.null");
                checkNotNull(middleOrderInfo.getOrderOriginFee(), "order.orderOriginFee.not.null");
                checkNotNull(middleOrderInfo.getDiscount(), "order.discount.not.null");
                checkNotNull(middleOrderInfo.getSkuCode(), "order.skuCode.not.null");
                checkNotNull(middleOrderInfo.getQuantity(), "order.quantity.not.null");
                checkNotNull(middleOrderInfo.getOriginFee(), "order.originFee.not.null");
                checkNotNull(middleOrderInfo.getItemDiscount(), "order.itemDiscount.not.null");

                //导入需求，订单金额类数字传入小数，精确到两位数字   转换成整型以分为单位
                middleOrderInfo.setShipFee(checkNumber(middleOrderInfo.getShipFee()).toString());
                middleOrderInfo.setFee(checkNumber(middleOrderInfo.getFee()).toString());
                middleOrderInfo.setOrderOriginFee(checkNumber(middleOrderInfo.getOrderOriginFee()).toString());
                middleOrderInfo.setDiscount(checkNumber(middleOrderInfo.getDiscount()).toString());
                middleOrderInfo.setOriginFee(checkNumber(middleOrderInfo.getOriginFee()).toString());
                middleOrderInfo.setItemDiscount(checkNumber(middleOrderInfo.getItemDiscount()).toString());

                orderInfos.add(middleOrderInfo);
            }
            long endTime = System.currentTimeMillis();
            log.info("analysis order import excel , date:{}", endTime - startTime);
        } catch (ServiceException | NullPointerException e) {
            throw new ServiceException(e.getMessage());
        } catch (Exception e) {
            log.error("analysis order import excel fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("analysis.order.import.excel.fail");
        } finally {
            bufferedInputStream.close();
        }
        return orderInfos;
    }


    private Boolean checkDataEmpty(String[] strs) {
        Integer count = 0;
        for (String str : strs) {
            if (!Strings.isNullOrEmpty(str)) {
                count++;
            }
        }
        return count == 0;
    }

    /**
     * 类型必须为double类型，并且精确两位小数
     *
     * @param str
     * @return 返回*100整数数字
     */
    public Integer checkNumber(String str) {
        try {
            Double number = Double.parseDouble(str);
            String[] strings = str.split("\\.");
            if (strings.length == 2) {
                if (strings[1].length() > 2) {
                    throw new ServiceException("order.fee.not.legal");
                }
            }
            Double fee = number * 100;
            return fee.intValue();
        } catch (Exception e) {
            throw new ServiceException("order.fee.not.legal");
        }
    }

    public String extractCellValue(Cell cell) {
        String value = "";
        try {
            if (cell == null) {
                return null;
            }
            switch (cell.getCellTypeEnum()) {
                case STRING:
                    value = cell.getStringCellValue();
                    break;
                case NUMERIC:
                    DecimalFormat df = new DecimalFormat("0");
                    value = String.valueOf(df.format(cell.getNumericCellValue()));
                    break;
                case FORMULA:
                    break;
                case BLANK:
                    break;
                case BOOLEAN:
                    break;
                default:
                    value = "";
            }
        } catch (Exception e) {
            System.out.println();
            log.error("extractCellValue failed");
        }
        return value;
    }

    public List<T> handlerCsv(InputStream ins, Class<? extends T> t) {
        InputStreamReader inputStreamReader = new InputStreamReader(ins);
        return handlerCsv(inputStreamReader, t);
    }

    //解析
    public static void checkCsv(String fileName) {
        if (!fileName.endsWith(".csv")) {
            throw new JsonResponseException("need.csv");
        }
    }

    public void writerCsv(List<T> list, String fileName) {

        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("UTF-8"));
            writer.write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}));
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
            beanToCsv.write(list);
            log.info("create file path=:{}", fileName);
            System.out.println("create file path=" + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CsvRequiredFieldEmptyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvDataTypeMismatchException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void writerUserExcel(List<MiddleOrderInfo> orderInfos, String fileName) {
        try {
            Workbook wb = new SXSSFWorkbook();
            Sheet sheet = wb.createSheet("订单导入模板");
            for (int i = 0; i < orderInfos.size(); i++) {
                Row row = sheet.createRow(i);
                Field[] field = orderInfos.get(i).getClass().getDeclaredFields();
                for (Integer j = 1; j < field.length; j++) {
                    String name = field[j].getName();
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    Method method = orderInfos.get(i).getClass().getMethod("get" + name);

                    Cell outerIdCell = row.createCell(j - 1, CellType.STRING);

                    outerIdCell.setCellValue(method.invoke(orderInfos.get(i)).toString());
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            wb.write(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            log.error("export order template fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "export.order.template.fail");
        }

    }


    public static List handlerExcel() {
        return null;
    }


    /**
     * 解析excel
     */
    private MiddleOrderInfo makeMiddleOrderInfo(String[] data, Field[] fields) {

        try {
            MiddleOrderInfo middleOrderInfo = new MiddleOrderInfo();
            for (Integer j = 1; j < fields.length; j++) {
                //把excel数据导入到bean类
                String name = fields[j].getName();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                Method method = middleOrderInfo.getClass().getMethod("set" + name, fields[j].getType());

                method.invoke(middleOrderInfo, data[j - 1] != "" ? data[j - 1] : null);
            }
            return middleOrderInfo;
        } catch (Exception e) {
            log.error("analysis order import excel fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("analysis.order.import.excel.fail");
        }
    }
}