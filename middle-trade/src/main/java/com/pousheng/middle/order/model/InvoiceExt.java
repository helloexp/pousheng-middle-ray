package com.pousheng.middle.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * Created by tony on 2017/8/3.
 * pousheng-middle
 */
public class InvoiceExt implements java.io.Serializable{
    private static final long serialVersionUID = 6479338217469274922L;
    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private Long id;
    @JsonIgnore
    private String detailJson;
    private Map<String, String> detail;

    public InvoiceExt(){

    }
    public void setDetailJson(String detailJson) throws Exception {
        this.detailJson = detailJson;
        if(Strings.isNullOrEmpty(detailJson)) {
            this.detail = Collections.emptyMap();
        } else {
            this.detail = (Map)objectMapper.readValue(detailJson, JacksonType.MAP_OF_STRING);
        }

    }

    public void setDetail(Map<String, String> detail) {
        this.detail = detail;
        if(detail != null && !detail.isEmpty()) {
            try {
                this.detailJson = objectMapper.writeValueAsString(detail);
            } catch (Exception var3) {
                ;
            }
        } else {
            this.detailJson = null;
        }

    }
    public String getDetailJson() {
        return this.detailJson;
    }

    public Map<String, String> getDetail() {
        return this.detail;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
