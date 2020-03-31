package com.pousheng.middle.consume.index.processor.impl.sendRule.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 默认发货车规则
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-13 11:47<br/>
 */
@Data
public class StockSendDocument implements Serializable {
    private static final long serialVersionUID = -8383408184668836830L;

    private Object id;
    private Long ruleId;
    private Long shopId;
    private String shopName;
    private String shopOutCode;
    private String companyCode;
    //线下门店是1，线上门店是2，根据shopName判断，mpos开头是线下
    private Long shopType;

    private Long zoneId;
    private String zoneName;

    private Long warehouseId;
    private String warehouseName;
    private String warehouseOutCode;
    private String warehouseCompanyCode;
    private Integer warehouseType;
    private Integer warehouseStatus;
}
