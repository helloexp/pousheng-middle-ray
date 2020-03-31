package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: wenchao.he
 * Desc:
 * Date: 2019/8/26
 */
@Data
public class RefundWarehouseRules implements Serializable {

    private static final long serialVersionUID = 332635333335456058L;
    
    private Long id;

    private Long shopId;
    
    private String orderShopCode;

    private String orderShopName;
    
    private String shipmentCompanyId;
    
    private String refundWarehouseCode;
    
    private Date createdAt;
    
    private Date updatedAt;
}
