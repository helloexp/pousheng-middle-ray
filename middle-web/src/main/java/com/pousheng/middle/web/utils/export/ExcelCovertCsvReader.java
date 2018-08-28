package com.pousheng.middle.web.utils.export;

import io.terminus.common.exception.ServiceException;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xieqinghe .
 * @date 2018/4/9 上午9:42
 * @email xieqinghe@terminus.io
 */
public class ExcelCovertCsvReader {

    public static void main(String[] args) throws Exception {
//        List<String[]> list = ExcelCovertCsvReader
//                .readerExcel(
//                        "/Users/hehe/Desktop/订单规则模板.xlsx",
//                        "Sheet1", 38,2000);
//
//        System.out.println(list.size());
//        for (String[] record : list) {
//            for (String cell : record) {
//                System.out.print(cell + "  ");
//            }
//            System.out.println();
//        }

        long startTime = System.currentTimeMillis();
        //////////////////////////////////
//        List<String[]> list2 = ExcelCovertCsvReader
//                .readerExcelAt(
//                        "/Users/hehe/Desktop/订单规则模板.xlsx", 38);
//        System.out.println(list2.size());

        long endTime = System.currentTimeMillis();

        System.out.println("time :" + (endTime - startTime));

        ///11997行数据  time :5064ms
    }


    enum xssfDataType {
        BOOL, ERROR, FORMULA, INLINESTR, SSTINDEX, NUMBER,
    }

    class MyXSSFSheetHandler extends DefaultHandler {

        private StylesTable stylesTable;
        private ReadOnlySharedStringsTable sharedStringsTable;
        private final PrintStream output;
        private final int minColumnCount;
        private boolean vIsOpen;
        private xssfDataType nextDataType;
        private short formatIndex;
        private String formatString;
        private final DataFormatter formatter;
        private int thisColumn = -1;
        private int lastColumnNumber = -1;
        private StringBuffer value;
        private String[] record;
        private List<String[]> rows = new ArrayList<String[]>();
        private boolean isCellNull = false;

        /**
         * Accepts objects needed while parsing.
         *
         * @param styles  Table of styles
         * @param strings Table of shared strings
         * @param cols    Minimum number of columns to show
         * @param target  Sink for output
         */
        public MyXSSFSheetHandler(StylesTable styles,
                                  ReadOnlySharedStringsTable strings, int cols, PrintStream target) {
            this.stylesTable = styles;
            this.sharedStringsTable = strings;
            this.minColumnCount = cols;
            this.output = target;
            this.value = new StringBuffer();
            this.nextDataType = xssfDataType.NUMBER;
            this.formatter = new DataFormatter();
            record = new String[this.minColumnCount];
            rows.clear();// 每次读取都清空行集合
        }

        @Override
        public void startElement(String uri, String localName, String name,
                                 Attributes attributes) throws SAXException {

            if ("inlineStr".equals(name) || "v".equals(name)) {
                vIsOpen = true;
                // Clear contents cache
                value.setLength(0);
            }
            // c => cell
            else if ("c".equals(name)) {
                // Get the cell reference
                String r = attributes.getValue("r");
                int firstDigit = -1;
                for (int c = 0; c < r.length(); ++c) {
                    if (Character.isDigit(r.charAt(c))) {
                        firstDigit = c;
                        break;
                    }
                }
                thisColumn = nameToColumn(r.substring(0, firstDigit));

                // Set up defaults.
                this.nextDataType = xssfDataType.NUMBER;
                this.formatIndex = -1;
                this.formatString = null;
                String cellType = attributes.getValue("t");
                String cellStyleStr = attributes.getValue("s");
                if ("b".equals(cellType)) {
                    nextDataType = xssfDataType.BOOL;
                } else if ("e".equals(cellType)) {
                    nextDataType = xssfDataType.ERROR;
                } else if ("inlineStr".equals(cellType)) {
                    nextDataType = xssfDataType.INLINESTR;
                } else if ("s".equals(cellType)) {
                    nextDataType = xssfDataType.SSTINDEX;
                } else if ("str".equals(cellType)) {
                    nextDataType = xssfDataType.FORMULA;
                } else if (cellStyleStr != null) {
                    int styleIndex = Integer.parseInt(cellStyleStr);
                    XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
                    this.formatIndex = style.getDataFormat();
                    this.formatString = style.getDataFormatString();
                    if (this.formatString == null) {
                        this.formatString = BuiltinFormats
                                .getBuiltinFormat(this.formatIndex);
                    }
                }
            }

        }

        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            String thisStr = null;
            if ("v".equals(name)) {
                switch (nextDataType) {
                    case BOOL:
                        char first = value.charAt(0);
                        thisStr = first == '0' ? "FALSE" : "TRUE";
                        break;
                    case ERROR:
                        thisStr = "ERROR:" + value.toString();
                        break;
                    case SSTINDEX:
                        String sstIndex = value.toString();
                        try {
                            int idx = Integer.parseInt(sstIndex);
                            XSSFRichTextString rtss = new XSSFRichTextString(
                                    sharedStringsTable.getEntryAt(idx));
                            thisStr = rtss.toString();
                        } catch (NumberFormatException ex) {
                            output.println("Failed to parse SST index '" + sstIndex
                                    + "': " + ex.toString());
                        }
                        break;
                    case NUMBER:
                        String n = value.toString();
                        //时间格式(不转换)  避免double数值精度不正确
                        if (!HSSFDateUtil.isADateFormat(this.formatIndex, n) && this.formatString != null && n.length() > 0) {
                            n = formatter.formatRawCellContents(Double.parseDouble(n), this.formatIndex, this.formatString);
                        } else if (!HSSFDateUtil.isADateFormat(this.formatIndex, n) && n.length() > 0) {
                            n = formatter.formatRawCellContents(Double.parseDouble(n), this.formatIndex, "General");
                        }
                        //避免出现科学计算法
                        BigDecimal bigDecimal = new BigDecimal(n);
                        thisStr = bigDecimal.toPlainString();
                        break;
                    default:
                        thisStr = value.toString();
                        break;
                }

                if (lastColumnNumber == -1) {
                    lastColumnNumber = 0;
                }

                record[thisColumn] = thisStr;
                // Update column
                if (thisColumn > -1) {
                    lastColumnNumber = thisColumn;
                }
            } else if ("row".equals(name)) {
                if (minColumns > 0) {
                    // Columns are 0 based
                    if (lastColumnNumber == -1) {
                        lastColumnNumber = 0;
                    }
                    rows.add(record.clone());
                    if (rows.size() > maxSize) {
                        throw new ServiceException("order.import.excel.more.max.size");
                    }
                    isCellNull = false;
                    for (int i = 0; i < record.length; i++) {
                        record[i] = null;
                    }
                }
                lastColumnNumber = -1;
            }
        }

        public List<String[]> getRows() {
            return rows;
        }

        public void setRows(List<String[]> rows) {
            this.rows = rows;
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (vIsOpen) {
                value.append(ch, start, length);
            }

        }

        private int nameToColumn(String name) {
            int column = -1;
            for (int i = 0; i < name.length(); ++i) {
                int c = name.charAt(i);
                column = (column + 1) * 26 + c - 'A';
            }
            return column;
        }
    }

    // /////////////////////////////////////

    private OPCPackage xlsxPackage;
    private int minColumns;
    private PrintStream output;
    private String sheetName;
    private Integer maxSize = 2000;

    /**
     * Creates a new XLSX -> CSV converter
     *
     * @param pkg        The XLSX package to process
     * @param output     The PrintStream to output the CSV to
     * @param minColumns The minimum number of columns to output, or -1 for no minimum
     */
    public ExcelCovertCsvReader(OPCPackage pkg, PrintStream output,
                                String sheetName, int minColumns, int maxSize) {
        this.xlsxPackage = pkg;
        this.output = output;
        this.minColumns = minColumns;
        this.sheetName = sheetName;
        this.maxSize = maxSize;
    }

    /**
     * Creates a new XLSX -> CSV converter
     *
     * @param pkg        The XLSX package to process
     * @param output     The PrintStream to output the CSV to
     * @param minColumns The minimum number of columns to output, or -1 for no minimum
     */
    public ExcelCovertCsvReader(OPCPackage pkg, PrintStream output,
                                String sheetName, int minColumns) {
        this.xlsxPackage = pkg;
        this.output = output;
        this.minColumns = minColumns;
        this.sheetName = sheetName;
    }

    /**
     * Parses and shows the content of one sheet using the specified styles and
     * shared-strings tables.
     *
     * @param styles
     * @param strings
     * @param sheetInputStream
     */
    public List<String[]> processSheet(StylesTable styles,
                                       ReadOnlySharedStringsTable strings, InputStream sheetInputStream)
            throws IOException, ParserConfigurationException, SAXException {

        InputSource sheetSource = new InputSource(sheetInputStream);
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        XMLReader sheetParser = saxParser.getXMLReader();
        MyXSSFSheetHandler handler = new MyXSSFSheetHandler(styles, strings,
                this.minColumns, this.output);
        sheetParser.setContentHandler(handler);
        sheetParser.parse(sheetSource);
        return handler.getRows();
    }

    /**
     * 初始化这个处理程序 将
     *
     * @throws IOException
     * @throws OpenXML4JException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public List<String[]> processAt() throws IOException, OpenXML4JException,
            ParserConfigurationException, SAXException {

        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(
                this.xlsxPackage);
        XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
        List<String[]> list = null;
        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader
                .getSheetsData();

        if (iter.hasNext()) {
            InputStream stream = iter.next();
            list = processSheet(styles, strings, stream);
            stream.close();
        }

        return list;
    }


    /**
     * 初始化这个处理程序 将
     *
     * @throws IOException
     * @throws OpenXML4JException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public List<String[]> process() throws IOException, OpenXML4JException,
            ParserConfigurationException, SAXException {

        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(
                this.xlsxPackage);
        XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
        List<String[]> list = null;
        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader
                .getSheetsData();
        int index = 0;
        while (iter.hasNext()) {
            InputStream stream = iter.next();
            String sheetNameTemp = iter.getSheetName();
            if (this.sheetName.equals(sheetNameTemp)) {
                list = processSheet(styles, strings, stream);
                stream.close();
                ++index;
            }
        }

        return list;
    }

    /**
     * 读取Excel
     *
     * @param path       文件路径
     * @param sheetName  sheet名称
     * @param minColumns 列总数
     * @return
     */
    private static List<String[]> readerExcel(String path, String sheetName,
                                              int minColumns) throws IOException, OpenXML4JException,
            ParserConfigurationException, SAXException {
        OPCPackage p = OPCPackage.open(path, PackageAccess.READ);
        ExcelCovertCsvReader xlsx2csv = new ExcelCovertCsvReader(p, System.out,
                sheetName, minColumns);
        List<String[]> list = xlsx2csv.process();
        p.close();
        return list;
    }

    /**
     * 读取Excel的第一个sheet
     *
     * @param in         文件
     * @param minColumns 列总数
     * @param maxSize    限制总行数
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws OpenXML4JException
     * @throws IOException
     * @throws ServiceException
     */
    public static List<String[]> readerExcelAt(InputStream in,
                                               int minColumns,
                                               int maxSize) throws IOException, OpenXML4JException,
            ParserConfigurationException, SAXException {

        OPCPackage p = OPCPackage.open(in);
        ExcelCovertCsvReader xlsx2csv = new ExcelCovertCsvReader(p, System.out,
                "", minColumns, maxSize);
        List<String[]> list = xlsx2csv.processAt();
        p.close();
        return list;
    }


}
