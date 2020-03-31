package com.pousheng.middle.consume.index.processor.impl.order.dto;

import lombok.Data;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-17 11:46<br/>
 */
@Data
public class OrderDocument {
    private Long id;
    private String outId;
    private String outFrom;
    private String orderCode;
    private Long companyId;
    private Long shopId;
    private String shopName;
    private String buyerName;
    private String buyerPhone;
    private Integer ecpOrderStatus;
    private Integer handleStatus;
    private Integer status;
    private String buyerNode;
    private String outCreatedAt;
    private String updatedAt;
    private String createdAt;
}
