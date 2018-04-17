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
 * Created by penghui on 2018/1/15
 * 自动补偿任务
 */
@Data
public class AutoCompensation implements Serializable{

    private static final long serialVersionUID = -3237750766179930751L;

    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    private Long id;

    /**
     * 任务类型
     */
    private Integer type;

    /**
     * 额外信息,持久化到数据库
     */
    @JsonIgnore
    private String extraJson;

    /**
     * 额外信息,不持久化
     */
    private Map<String,String> extra;

    /**
     * 处理状态 0：未处理 1：已处理
     */
    private Integer status;

    /**
     * 次数
     */
    private Integer time;

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
