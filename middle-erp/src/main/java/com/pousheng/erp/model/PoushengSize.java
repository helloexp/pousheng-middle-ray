package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@Data
public class PoushengSize implements Serializable {
    private static final long serialVersionUID = -5933311920006296524L;

    private String size_id; //尺码ID

    private String size_code; //尺码代码

    private String size_name; //尺码名称

    private String bar_code; //条码
}
