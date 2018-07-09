package com.pousheng.middle.task.dto;

import java.util.Map;

/**
 * @author zhaoxw
 * @date 2018/5/10
 * 用于记录分组操作的所有信息，
 */
public class ItemGroupTask {

    Map<String,String> params;

    private Long groupId;

    private Boolean mark;

    private Integer type;

    private Long userId;

    private String fileUrl;

    private Long createdAt;

    public Map<String, String> getParams() {
        return params;
    }

    public ItemGroupTask params(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public ItemGroupTask groupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }

    public Boolean getMark() {
        return mark;
    }

    public ItemGroupTask mark(Boolean mark) {
        this.mark = mark;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public ItemGroupTask type(Integer type) {
        this.type = type;
        return this;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public ItemGroupTask fileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public ItemGroupTask userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public ItemGroupTask createdAt(Long createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
