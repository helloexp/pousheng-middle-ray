package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 *
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
@Data
public class PoushengCompensateBizCriteria  extends PagingCriteria implements Serializable {

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
     * 状态
     */
    private String status;

    /**
     * 失败次数
     */
    private Integer cnt;

    /**
     * 忽略失败次数
     */
    private Integer ignoreCnt;

}
