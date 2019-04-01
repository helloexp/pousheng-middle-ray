package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/28
 * pousheng-middle
 * @author tony
 */
@Data
public class PoushengGiftActivityCriteria extends PagingCriteria implements java.io.Serializable{
    private static final long serialVersionUID = 4282263665769465196L;

    private List<Integer> statuses;

    /**
     * 活动编号
     */
    private Long id;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动状态
     */
    private Integer status;
}
