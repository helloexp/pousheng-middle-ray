package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xjn on 17/5/9.
 * 原类别
 */
@Data
public class PoushengKind implements Serializable {

    private static final long serialVersionUID = 2918285625050361551L;
    private String kindID;
    private String kindCode;
    private String kindName;
    private String remark;
    private Boolean allowUsed;
    private Date modifyDTM;
    private Integer lan;

}
