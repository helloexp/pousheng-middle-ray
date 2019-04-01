package com.pousheng.middle.warehouse.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: zhaoxiaowei
 * Desc: Model类
 * Date: 2018-09-04
 */
@Data
public class WarehouseRulePriority implements Serializable {

    private static final long serialVersionUID = -790064182868113971L;

    private Long id;

    private Long ruleId;

    /**
     * 规则名称
     */
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+08:00")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+08:00")
    private Date endDate;

    /**
     * 1启用 -1禁用
     */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
