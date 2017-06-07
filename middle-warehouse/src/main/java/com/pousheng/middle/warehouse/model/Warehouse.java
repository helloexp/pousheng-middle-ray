package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: 仓库Model类
 * Date: 2017-06-07
 */
@Data
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 7864298121373633591L;
    private Long id;
    
    /**
     * 仓库编码
     */
    private String code;
    
    /**
     * 仓库名称
     */
    private String name;
    
    /**
     * 负责人id
     */
    private Long ownerId;
    
    /**
     * 是否默认发货仓
     */
    private Boolean isDefault;
    
    /**
     * 附加信息
     */
    private String extraJson;
    
    private Date createdAt;
    
    private Date updatedAt;
}
