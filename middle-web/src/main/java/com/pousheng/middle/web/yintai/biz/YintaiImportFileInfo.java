package com.pousheng.middle.web.yintai.biz;

import lombok.Data;

import java.io.Serializable;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/25
 */
@Data
public class YintaiImportFileInfo implements Serializable {
    private static final long serialVersionUID = 2515392165049935446L;

    private String filePath;

    private String fileName;

    private Long userId;
}
