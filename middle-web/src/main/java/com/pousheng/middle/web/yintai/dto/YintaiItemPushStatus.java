package com.pousheng.middle.web.yintai.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/18
 */
@Data
@Builder
public class YintaiItemPushStatus implements Serializable {

    private static final long serialVersionUID = 6490290232700555423L;
    private String name;

    private Integer code;

}
