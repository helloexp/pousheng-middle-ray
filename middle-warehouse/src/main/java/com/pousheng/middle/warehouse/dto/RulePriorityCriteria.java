package com.pousheng.middle.warehouse.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.Date;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Data
public class RulePriorityCriteria extends PagingCriteria  {

    private String name;

    private Long ruleId;

    private Integer status;

    private Date searchDate;

    /**
     * 开始时间
     */
    private Date startAt;

    /**
     * 结束时间
     */
    private Date endAt;



    public String getName() {
        return name;
    }

    public RulePriorityCriteria name(String name) {
        this.name = name;
        return this;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public RulePriorityCriteria ruleId(Long ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public RulePriorityCriteria status(Integer status) {
        this.status = status;
        return this;
    }

    public Date getSearchDate() {
        return searchDate;
    }

    public RulePriorityCriteria searchDate(Date searchDate) {
        this.searchDate = searchDate;
        return this;
    }

    public Date getStartAt() {
        return startAt;
    }

    public RulePriorityCriteria startAt(Date startAt) {
        this.startAt = startAt;
        return this;
    }

    public Date getEndAt() {
        return endAt;
    }

    public RulePriorityCriteria endAt(Date endAt) {
        this.endAt = endAt;
        return this;
    }
}
