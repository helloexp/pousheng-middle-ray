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
    private String cardID;
    private String cardCode;
    private String cardName;
    private String fullName;
    private String foreignName;
    private String remark;
    private Boolean allowUsed;
    private Date modifyDTM;
    private Boolean lan;
}
