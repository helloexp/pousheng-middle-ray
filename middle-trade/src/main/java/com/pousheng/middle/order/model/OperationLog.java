package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: sunbo
 * Desc: Model类
 * Date: 2017-07-31
 */
@Data
public class OperationLog implements Serializable {
    //TODO: Do not forget add "serialVersionUID" field AND change package path!

    private Long id;
    
    /**
     * 类型
     */
    private Integer type;
    
    /**
     * 操作人名
     */
    private String operatorName;
    
    /**
     * 操作的实体ID
     */
    private String operateId;
    
    /**
     * 操作内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
}
