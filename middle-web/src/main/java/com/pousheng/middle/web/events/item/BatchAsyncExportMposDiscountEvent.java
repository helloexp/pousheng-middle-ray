package com.pousheng.middle.web.events.item;

import lombok.Data;

import java.util.Map;

/**
 * 异步导出mpos折扣事件
 * author penghui
 * @since 2017-12-13
 */
@Data
public class BatchAsyncExportMposDiscountEvent {

    private Map<String,String> params;

    private Long currentUserId;

}
