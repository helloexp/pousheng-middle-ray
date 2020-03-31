package com.pousheng.middle.task.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: songrenfei
 * Desc: 任务信息表Model类
 * Date: 2018-05-11
 */
@Data
public class ScheduleTask implements Serializable {

    private Long id;

    /**
     * 任务类型
     */
    private Integer type;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 业务id
     */
    private Long businessId;

    /**
     * 业务类型
     */
    private Integer businessType;


    /**
     * 当前状态
     */
    private Integer status;

    /**
     * 定时任务的相关参数
     */
    private String extraJson;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public ScheduleTask id(Long id) {
        this.id = id;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public ScheduleTask type(Integer type) {
        this.type = type;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public ScheduleTask userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public ScheduleTask businessId(Long businessId) {
        this.businessId = businessId;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public ScheduleTask status(Integer status) {
        this.status = status;
        return this;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public ScheduleTask extraJson(String extraJson) {
        this.extraJson = extraJson;
        return this;
    }

    public String getResult() {
        return result;
    }

    public ScheduleTask result(String result) {
        this.result = result;
        return this;
    }


    public Integer getBusinessType() {
        return businessType;
    }

    public ScheduleTask businessType(Integer businessType) {
        this.businessType = businessType;
        return this;
    }


    public Date getCreatedAt() {
        return createdAt;
    }

    public ScheduleTask createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public ScheduleTask updatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
