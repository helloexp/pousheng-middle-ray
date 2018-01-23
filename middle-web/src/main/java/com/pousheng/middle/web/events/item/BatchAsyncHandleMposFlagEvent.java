package com.pousheng.middle.web.events.item;

import lombok.Data;

import java.util.Map;

/**
 * 批量异步打标,取消打标事件
 * @author penghui
 * @since 2017/12/11
 */
@Data
public class BatchAsyncHandleMposFlagEvent {

    private Map<String,String> params;

    private String type;

    private Long currentUserId;

}
