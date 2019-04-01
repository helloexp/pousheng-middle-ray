package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Refundtemplates implements Serializable {
    
    private static final long serialVersionUID = -8739512197871510849L;
    private Long id;
    private String batchCode;
    private String saleCode;
    private Integer refundType;
    private String shipCode;
    private String skuCode;
    private Integer num;
    private Integer applyStatus = 0;
    private Date createdAt;
    private Date updatedAt;
}
