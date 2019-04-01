package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xjn on 17/5/9.
 * 原款型
 */
@Data
public class PoushengModel implements Serializable {

    private static final long serialVersionUID = -3970510292312106679L;
    private String modelID;
    private String modelCode;
    private String modelName;
    private String remark;
    private Integer allowUsed;
    private Date modifyDTM;
    private Integer lan;
}
