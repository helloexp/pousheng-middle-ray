package com.pousheng.middle.warehouse.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
public class SkuStockUpdated implements Serializable {

    private static final long serialVersionUID = 4291293525147003215L;

    private Long id;

    private Long taskId;

    private String skuCode;

    private Date createdAt;

    private Date updatedAt;

}
