package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xjn on 17/5/9.
 * 原品牌
 */
@Data
public class PoushengCard implements Serializable {

    private static final long serialVersionUID = 2723092187930687915L;
    private String card_id;
    private String card_code;
    private String card_name;
    private String full_name;
    private String remark;
}
