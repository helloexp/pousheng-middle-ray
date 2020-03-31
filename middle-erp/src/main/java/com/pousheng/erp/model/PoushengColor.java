package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@Data
public class PoushengColor implements Serializable {
    private static final long serialVersionUID = -7910798888095801326L;

    private String colorId; //颜色ID

    private String colorCode; //颜色代码

    private String colorName; //颜色名称

    private String remark; //颜色备注

    private Boolean allowUsed; // 是否允许使用

    private Date updatedAt; //修改时间
}
