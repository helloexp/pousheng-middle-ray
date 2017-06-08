/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.warehouse.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 菜鸟地址
 *
 * Author  : panxin
 * Date    : 12:23 PM 3/5/16
 * Mail    : panxin@terminus.io
 */
@ToString(of = {"id", "pid", "name", "level"})
@EqualsAndHashCode(of = {"id", "pid", "name", "level"})
public class WarehouseAddress implements Serializable {

    private static final long serialVersionUID = -7496545307904479697L;

    @Getter
    @Setter
    private Long id;            //主键

    @Getter
    @Setter
    private Long pid;           //父级id

    @Getter
    @Setter
    private String name;        //名称

    @Getter
    @Setter
    private Integer level;      //级别

    @Getter
    @Setter
    private String pinyin;      //拼音

    @Getter
    @Setter
    private String englishName; //英文名

    @Getter
    @Setter
    private String unicodeCode; //unicode

    @Getter
    @Setter
    private String orderNo;     //排序号

}
