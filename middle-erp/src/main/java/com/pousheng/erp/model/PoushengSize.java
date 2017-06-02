package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@Data
public class PoushengSize implements Serializable {
    private static final long serialVersionUID = -5933311920006296524L;

    private String sizeId; //尺码ID

    private String sizeCode; //尺码代码

    private String sizeName; //尺码名称

    private String barCode; //条码

    private String remark; //备注

    private Boolean allowUsed; //是否允许使用

    private Date updatedAt; //修改时间

}
