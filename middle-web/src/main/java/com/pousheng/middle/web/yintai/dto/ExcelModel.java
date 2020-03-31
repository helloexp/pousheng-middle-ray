package com.pousheng.middle.web.yintai.dto;

import lombok.Data;

import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/24
 */
@Data
public class ExcelModel {
    /**
     * 工作表名
     */
    private String sheetName;
    /**
     * 表内数据,保存在二维的ArrayList对象中
     */
    private List<List<String>> data;
    /**
     * 数据表的标题内容
     */
    private List<String> header;

}
