package com.pousheng.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Author: songrenfei
 * Desc: 用户基本信息表Model类
 * Date: 2017-06-23
 */
public class User implements Serializable {

    private static final long serialVersionUID = -6094139869714274527L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    @Getter
    @Setter
    private Long id;
    
    /**
     * 外部用户 id
     */
    @Getter
    @Setter
    private Long outId;
    
    /**
     * 用户名
     */
    @Getter
    @Setter
    private String name;
    
    /**
     * 手机号码
     */
    @Getter
    @Setter
    private String mobile;
    
    /**
     * 放店铺扩展信息,不存数据库
     */
    @Getter
    private Map<String, String> extra;

    /**
     * 放用户扩展信息, json存储, 存数据库
     */
    @Getter
    @JsonIgnore
    private String extraJson;

    @Getter
    @Setter
    private Date createdAt;

    @Getter
    @Setter
    private Date updatedAt;



    public void setExtraJson(String extraJson) throws Exception{
        this.extraJson = extraJson;
        if(Strings.isNullOrEmpty(extraJson)){
            this.extra= Collections.emptyMap();
        } else{
            this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if(extra ==null ||extra.isEmpty()){
            this.extraJson = null;
        }else{
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }
}
