package com.pousheng.middle.web.async;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * AUTHOR: zhangbin
 * ON: 2019/5/5
 */
@Data
@ApiModel("任务状态")
public class TaskResponse implements Serializable {
    private static final long serialVersionUID = -2169581270887407173L;
    @ApiModelProperty("任务起始时间")
    private Date initDate;
    @ApiModelProperty("当前状态")
    private String status;
    @ApiModelProperty("类型")
    private String type;

    private Map content;

    public static TaskResponse empty() {
        TaskResponse response = new TaskResponse();
        response.setInitDate(null);
        response.setStatus(null);
        response.setContent(ImmutableMap.of("msg", "无正在运行任务"));
        return response;
    }
}
