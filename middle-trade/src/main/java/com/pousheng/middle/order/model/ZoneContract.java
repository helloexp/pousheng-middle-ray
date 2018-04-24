package com.pousheng.middle.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Date;

import static com.pousheng.middle.order.constant.TradeConstants.STATUS_ENABLE;

/**
 * Author: songrenfei
 * Desc: 区部联系人表Model类
 * Date: 2018-04-04
 */
@Data
public class ZoneContract implements Serializable {
    //TODO: Do not forget add "serialVersionUID" field AND change package path!

    private Long id;

    /**
     * 区部id
     */
    private String zoneId;

    /**
     * 区部名称
     */
    private String zoneName;

    /**
     * 联系人姓名
     */
    private String name;

    /**
     * 联系人邮箱
     */
    private String email;

    /**
     * 联系人电话
     */
    private String phone;

    /**
     * 分组
     */
    private Integer group;

    /**
     * 状态
     */
    private Integer status=STATUS_ENABLE;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 开始时间
     */
    @JsonIgnore
    private Date startAt;
    /**
     * 结束时间
     */
    @JsonIgnore
    private Date endAt;
}
