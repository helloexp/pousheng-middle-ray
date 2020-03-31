package com.pousheng.middle.web.events.item;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 异步货品批量分组
 * @author zhaoxw
 * @date 2018/5/2
 */
@Data
public class BatchAsyncImportGroupEvent {

    private MultipartFile file;

    private Long currentUserId;

    private Long groupId;

    private Integer type;

}
