package com.pousheng.middle.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
@Data
public class OpenPushOrderTask implements java.io.Serializable{

    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private Long id;
    /**
     * 来源单号
     */
    private String sourceOrderId;
    /**
     * 渠道
     */
    private String channel;
    /**
     * 状态:0未处理，1.已处理
     */
    private Integer status;

    /**
     * 额外信息,持久化到数据库
     */
    @JsonIgnore
    protected String extraJson;

    /**
     * 额外信息,不持久化到数据库
     */
    protected Map<String, String> extra;

    private Date createdAt;

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

            }
        }
    }
}
