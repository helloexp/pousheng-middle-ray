package com.pousheng.middle.web.events.item;

import lombok.Data;

import java.io.File;

/**
 * 异步导入mpos折扣事件
 * @author penghui
 * @since 2017-12-13
 */
@Data
public class BatchAsyncImportMposDiscountEvent {

    private File file;

}
