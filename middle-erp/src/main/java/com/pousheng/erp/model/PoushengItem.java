package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 * 原项目
 */
@Data
public class PoushengItem implements Serializable {

    private static final long serialVersionUID = -4155235510616099050L;

    private String itemID;
    private String itemCode;
    private String itemName;
    private String remark;
    private Boolean allowUsed;
    private Date modifyDTM;
    private Integer lan;

}
