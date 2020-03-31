package com.pousheng.middle.web.yintai.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/18
 */
@Data
public class RecordLog implements Serializable {
    private static final long serialVersionUID = -1531274772501761555L;

    private List<YintaiPushItemDTO> pushItems;

    private String cause;

    public RecordLog(List<YintaiPushItemDTO> pushItems, String cause) {
        this.pushItems = pushItems;
        this.cause = cause;
    }
}
