package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表Model类
 * Date: 2017-12-15
 */
@Data
public class AddressGps implements Serializable {

    private static final long serialVersionUID = -8972306911812935066L;
    private Long id;
    
    /**
     * 业务ID
     */
    private Long businessId;
    
    /**
     * 业务类型，1：门店，2：仓库
     */
    private Integer businessType;

    /**
     * 经度
     */
    private String longitude;
    
    /**
     * 纬度
     */
    private String latitude;
    
    /**
     * 省
     */
    private String province;
    
    /**
     * 省ID
     */
    private Long provinceId;
    
    /**
     * 市
     */
    private String city;
    
    /**
     * 市ID
     */
    private Long cityId;
    
    /**
     * 区
     */
    private String region;
    
    /**
     * 区ID
     */
    private Long regionId;
    
    /**
     * 街道，可以为空
     */
    private String street;
    
    /**
     * 街道ID，可以为空
     */
    private Long streetId;
    
    /**
     * 详细地址
     */
    private String detail;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
}
