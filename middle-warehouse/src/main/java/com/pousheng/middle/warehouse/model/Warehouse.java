package com.pousheng.middle.warehouse.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 仓库Model类
 * Date: 2017-06-07
 */
@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 7864298121373633591L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private static final TypeReference<Map<String, String>> MAP_OF_STRING = new TypeReference<Map<String,String>>(){};


    @Setter
    private Long id;
    
    /**
     * 仓库编码
     */
    @Setter
    private String code;
    
    /**
     * 仓库名称
     */
    @Setter
    private String name;

    /**
     * 公司代码
     */
    @Setter
    private String companyId;

    /**
     * 公司名称
     */
    @Setter
    private String companyName;
    
    /**
     * 负责人id
     */
    @Setter
    private Long ownerId;

    /**
     * 仓库类别 0 总仓 1 店仓
     */
    @Setter
    private Integer type;

    /**
     * 仓库状态
     */
    @Setter
    private Integer status;

    /**
     * 仓库地址
     */
    @Setter
    private String address;
    
    /**
     * 是否默认发货仓
     */
    @Setter
    private Boolean isDefault;

    /**
     * 是否mpos 1:mpos 0:非mpos
     */
    @Setter
    private Integer isMpos;
    
    /**
     * 附加信息
     */
    private String extraJson;


    private Map<String, String> extra;

    /**
     * tag信息
     */
    private String tagsJson;


    private Map<String,String> tags;


    @Setter
    private Date createdAt;

    @Setter
    private Date updatedAt;

    public void setExtraJson(String extraJson) throws Exception{
        this.extraJson = extraJson;
        if(Strings.isNullOrEmpty(extraJson)){
            this.extra= Collections.emptyMap();
        } else{
            this.extra = objectMapper.readValue(extraJson, MAP_OF_STRING);
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


    public void setTagsJson(String tagsJson) throws Exception{
        this.tagsJson = tagsJson;
        if(Strings.isNullOrEmpty(tagsJson)){
            this.tags= Collections.emptyMap();
        } else{
            this.tags = objectMapper.readValue(tagsJson, MAP_OF_STRING);
        }
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
        if(tags ==null ||tags.isEmpty()){
            this.tagsJson = null;
        }else{
            try {
                this.tagsJson = objectMapper.writeValueAsString(tags);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }

    /**
     * 获取公司编码
     *
     * @return 公司编码
     */
    public String getCompanyCode(){
        if(StringUtils.hasText(code)) {
            return Splitter.on('-').omitEmptyStrings().trimResults().limit(2).splitToList(code).get(0);
        }
        return null;
    }

    /**
     * 获取仓库内码
     *
     * @return 仓库内码
     */
    public String getInnerCode() {
        if (StringUtils.hasText(code)) {
            return Splitter.on('-').omitEmptyStrings().trimResults().limit(2).splitToList(code).get(1);
        }
        return null;
    }


    /**
     * 获取仓库内码
     *
     * @return 仓库内码
     */
    public String getOutCode() {
        Map<String, String>  extra = this.getExtra();
        if(CollectionUtils.isEmpty(extra)||!extra.containsKey("outCode")){
            log.error("warehouse(id:{}) out code invalid",this.getId());
            throw new ServiceException("warehouse.out.code.invalid");
        }
        return extra.get("outCode");
    }
}
