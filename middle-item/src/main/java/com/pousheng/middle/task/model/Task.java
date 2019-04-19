package com.pousheng.middle.task.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a>
 * @date 2019-04-09 15:33:31
 */
@Data
public class Task implements Serializable {

    private static final long serialVersionUID = 7724557785765923879L;

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 状态
     */
    private String status;

    /**
     * 类型
     */
    private String type;

    /**
     * 任务描述，应当精炼简介，尽量保存外部信息主键
     */
    private String contextJson;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}
