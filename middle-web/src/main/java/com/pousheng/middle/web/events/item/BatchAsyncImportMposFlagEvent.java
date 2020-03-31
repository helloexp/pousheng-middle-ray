package com.pousheng.middle.web.events.item;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 异步导入mpos打标事件
 * @author penghui
 * @since 2017-12-13
 */
@Data
public class BatchAsyncImportMposFlagEvent {

    private MultipartFile file;

    private Long currentUserId;

}
