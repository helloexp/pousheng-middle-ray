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
import com.pousheng.middle.common.utils.batchhandle.ExcelUtil;
import com.pousheng.middle.common.utils.batchhandle.ExcelUtilSkuGroupSpuImport;
import com.pousheng.middle.order.dto.MiddleOrderInfo;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.excel.aftersale.FileImportExcelBean;
import com.pousheng.middle.web.excel.aftersale.FileImportExcelBeanWrapper;
import com.pousheng.middle.web.utils.export.ExcelCovertCsvReader;
import com.pousheng.middle.web.warehouses.dto.PoushengChannelImportDTO;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
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

    public List<String[]> handleGroupRuleImportFile(String fileUrl) {
        List<String[]> list;
        try {
            URL url = new URL(fileUrl);
            InputStream insr = url.openConnection().getInputStream();
            list = ExcelUtilSkuGroupSpuImport.readerExcel(insr, 5);
            if (CollectionUtils.isEmpty(list)) {
                log.error("import excel is empty so skip");
                throw new JsonResponseException("excel.content.is.empty");
            }
            log.info("import excel size:{}", list.size());
        } catch (Exception e) {
            log.error("read import excel file fail,causeL:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("read.excel.fail");
        }
        return list;
    }

    public List<String[]> handle(String fileUrl) {

        List<String[]> list;
        try {
            URL url = new URL(fileUrl);
            InputStream insr = url.openConnection().getInputStream();
            list = ExcelUtil.readerExcel(insr,  5);
            if (CollectionUtils.isEmpty(list)) {
                log.error("import excel is empty so skip");
                throw new JsonResponseException("excel.content.is.empty");
            }
            log.info("import excel size:{}", list.size());
        } catch (Exception e) {
            log.error("read import excel file fail,causeL:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("read.excel.fail");
        }
        return list;
    }


    public List<String[]> handlerExcel(String fileUrl) {

        List<String[]> list;
        try {
            URL url = new URL(fileUrl);
            InputStream insr = url.openConnection().getInputStream();
            list = ExcelCovertCsvReader
                    .readerExcelAt(
                            insr, 6, 5001);
            if (CollectionUtils.isEmpty(list)) {
                log.error("import excel is empty so skip");
                throw new JsonResponseException("excel.content.is.empty");
            }
            log.info("import excel size:{}", list.size());
        } catch (Exception e) {
            log.error("read import excel file fail,causeL:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("read.excel.fail");
        }
        return list;
    }


    /**
     * 限制excel导入最大条数
     */
    private static final Integer MAX_SIZE = 2000;

    public List<PoushengChannelImportDTO> handlerExcelChannelInventory(InputStream insr) throws IOException {
        List<PoushengChannelImportDTO> channelDTOS = Lists.newArrayList();
        long startTime = System.currentTimeMillis();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(insr);
        try {
            //存在serialVersionUID，需要去掉
            Field[] fields = PoushengChannelImportDTO.class.getDeclaredFields();
            List<String[]> list = ExcelCovertCsvReader
                    .readerExcelAt(
                            bufferedInputStream, fields.length - 1, 501);
            for (Integer i = 1; i < list.size(); i++) {
                //判断空行
                if (checkDataEmpty(list.get(i))) {
                    continue;
                }
                PoushengChannelImportDTO channelImportDTO = makeChannelImportDTO(list.get(i), fields);
                if (Strings.isNullOrEmpty(channelImportDTO.getSkuCode())) {
                    throw new ServiceException("第"+i+"行：条码不能为空");
                }
                if (Strings.isNullOrEmpty(channelImportDTO.getShopOutCode())) {
                    throw new ServiceException("第"+i+"行：指定店铺标识不能为空");
                }
                if (Strings.isNullOrEmpty(channelImportDTO.getBizOutCode())) {
                    throw new ServiceException("第"+i+"行：仓库标识不能为空");
                }
                if (!isPositiveNumber(channelImportDTO.getChannelQuantity())) {
                    throw new ServiceException("第"+i+"行：指定库存数量必须为正整数");
                }

                channelDTOS.add(channelImportDTO);
            }
            long endTime = System.currentTimeMillis();
            log.info("analysis channel inventory import excel , date:{}", endTime - startTime);
        } catch (ServiceException | NullPointerException e) {
            throw new ServiceException(e.getMessage());
        } catch (Exception e) {
            log.error("analysis channel inventory import excel fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("analysis.channel.inventory.import.excel.fail");
        } finally {
            bufferedInputStream.close();
        }
        return channelDTOS;
    }


    /**
     * 处理售后单导入
     * @return
     * @throws IOException
     */
    public List<FileImportExcelBeanWrapper> handleAftersaleOrderExcel(String filePath) throws Exception {
        List<FileImportExcelBeanWrapper> resultData = Lists.newArrayList();
        BufferedInputStream bufferedInputStream = null;
        try {
            URL url = new URL(filePath);
            InputStream insr = url.openConnection().getInputStream();
            bufferedInputStream = new BufferedInputStream(insr);
            List<String[]> list = ExcelCovertCsvReader.readerExcelAt(bufferedInputStream, 5, 502);
            if (list != null && list.size() > 0) {
                if (list.size() > 501) {
                    throw new BizException("excel数据超过500条");
                }
                //TODOGF 验证表头
                //TODOGF 空行问题
                //从第二行开始获取数据
                for(Integer i = 1; i < list.size(); i++) {
                    FileImportExcelBeanWrapper fileImportExcelBeanWrapper = makeFileImportExcelBean(list.get(i));
                    if (fileImportExcelBeanWrapper == null) {
                        continue;
                    }
                    resultData.add(fileImportExcelBeanWrapper);
                }
            }
        } catch (Exception e) {
            log.error("读取售后单excel数据出错:{}", e.getMessage());
            e.printStackTrace();
            throw new BizException(e.getMessage());
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
        }
        return resultData;
    }

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

    public boolean isPositiveNumber(String input) {
        if (ObjectUtils.isEmpty(input)) {
            return false;
        }

        try {
            int out = Integer.parseInt(input);
            if (out <= 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
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

    public void writerExcel(List<String[]> rows, String fileName) {
        try {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("sheet1");
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i);

                for (int j = 0; j < rows.get(i).length; j++) {
                    Cell cell = row.createCell(j, CellType.STRING);
                    cell.setCellValue(rows.get(i)[j]);
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
    private PoushengChannelImportDTO makeChannelImportDTO(String[] data, Field[] fields) {

        try {
            PoushengChannelImportDTO channelImportDTO = new PoushengChannelImportDTO();
            for (Integer j = 1; j < fields.length; j++) {
                //把excel数据导入到bean类
                String name = fields[j].getName();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                Method method = channelImportDTO.getClass().getMethod("set" + name, fields[j].getType());
                method.invoke(channelImportDTO, data[j - 1] != "" ? data[j - 1] : null);
            }
            return channelImportDTO;
        } catch (Exception e) {
            log.error("analysis channel inventory import excel fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new ServiceException("analysis.channel.inventory.import.excel.fail");
        }
    }

    private FileImportExcelBeanWrapper makeFileImportExcelBean(String[] data) {
        if (data != null && data.length > 0) {
            int length = data.length;
            FileImportExcelBean bean = new FileImportExcelBean();
            FileImportExcelBeanWrapper wrapper = new FileImportExcelBeanWrapper();
            boolean hasError = false;
            for (int i = 0; i < length; i ++) {
                switch (i) {
                    case 0:
                        if (StringUtils.isEmpty(data[0])) {
                            hasError = true;
                            break;
                        }
                        bean.setOrderNumber(data[0].trim());
                        break;
                    case 1:
                        String type = data[1];
                        if (StringUtils.isEmpty(type)) {
                            hasError = true;
                            break;
                        } else {
                            type = type.trim();
                            bean.setType(type);
                            //验证类型是否错误，当前只支持2退货退款
                            if (! "2".equals(type)) {
                                hasError = true;
                            }
                        }
                        break;
                    case 2:
                        if (StringUtils.isEmpty(data[2])) {
                            hasError = true;
                            break;
                        } else {
                            bean.setShipmentOrderNumber(data[2].trim());
                        }
                        break;
                    case 3:
                        if (StringUtils.isEmpty(data[3])) {
                            hasError = true;
                            break;
                        } else {
                            bean.setBarCode(data[3].trim());
                        }
                        break;
                    case 4:
                        if (StringUtils.isEmpty(data[4])) {
                            hasError = true;
                            break;
                        } else {
                            //验证数字是否错误
                            try {
                                Integer quantity = Integer.valueOf(data[4].trim());
                                if (quantity > 0) {
                                    bean.setQuantity(quantity.toString());
                                    break;
                                }
                                hasError = true;
                            } catch (Exception e) {
                                hasError = true;
                            }
                        }
                        bean.setQuantity(data[4]);
                        break;
                }
            }
            if (hasError == true) {
                wrapper.setErrorMsg("导入的参数错误,请核验");
                wrapper.setHasError(true);
            }
            wrapper.setFileImportExcelBean(bean);
            return wrapper;
        } else {
            return null;
        }
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
