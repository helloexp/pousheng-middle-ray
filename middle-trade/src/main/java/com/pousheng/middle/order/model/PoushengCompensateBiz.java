package com.pousheng.middle.order.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 中台业务处理表
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@Data
public class PoushengCompensateBiz implements Serializable {

    private static final long serialVersionUID = -8225212884544416562L;
    /**
     * 主键
     */
    private Long id;

    /**
     * 业务id
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 明细内容
     */
    private String context;

    /**
     * 状态
     */
    private String status;

    /**
     * 失败次数
     */
    private Integer cnt;

    /**
     * 上次失败原因
     */
    private String lastFailedReason;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
