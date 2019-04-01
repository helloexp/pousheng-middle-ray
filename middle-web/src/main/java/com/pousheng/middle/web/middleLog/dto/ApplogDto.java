package com.pousheng.middle.web.middleLog.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/8/31
 */
@Data
public class ApplogDto {

    String operator;

    Date createdAt;

    String type;

    Map<String, String> detail;

    public String getOperator() {
        return operator;
    }

    public ApplogDto operator(String operator) {
        this.operator = operator;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ApplogDto createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getType() {
        return type;
    }

    public ApplogDto type(String type) {
        this.type = type;
        return this;
    }

    public Map<String, String> getDetail() {
        return detail;
    }

    public ApplogDto detail(Map<String, String> detail) {
        this.detail = detail;
        return this;
    }
}
