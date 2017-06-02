package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xjn on 17/5/9.
 * 原系列
 */
@Data
public class PoushengSeries implements Serializable {

    private static final long serialVersionUID = 5034464610517205510L;
    private String seriesID;
    private String seriesCode;
    private String seriesName;
    private String remark;
    private Boolean allowUsed;
    private Date modifyDTM;
    private Integer lan;
}
