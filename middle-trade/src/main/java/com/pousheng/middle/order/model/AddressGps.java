package com.pousheng.middle.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表Model类
 * Date: 2017-12-15
 */
@Data
public class AddressGps implements Serializable {

    private static final long serialVersionUID = -8972306911812935066L;

    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

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
     * 额外信息,持久化到数据库
     */
    @JsonIgnore
    protected String extraJson;

    /**
     * 额外信息,不持久化到数据库
     */
    protected Map<String, String> extra;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;


    public void setExtraJson(String extraJson) throws Exception {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra == null || extra.isEmpty()) {
            this.extraJson = null;
        } else {
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }
}
