package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhaoxw
 * @date 2018/8/9
 */
@Data
public class WarehouseRulePriorityImportInfo implements Serializable{

    private static final long serialVersionUID = 4075636810006283211L;

    private String filePath;

    private String fileName;

    private Long priorityId;

    private Long userId;

    public String getFileName() {
        return fileName;
    }

    public WarehouseRulePriorityImportInfo fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public WarehouseRulePriorityImportInfo filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public WarehouseRulePriorityImportInfo userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getPriorityId() {
        return priorityId;
    }

    public WarehouseRulePriorityImportInfo priorityId(Long priorityId) {
        this.priorityId = priorityId;
        return this;
    }
}
