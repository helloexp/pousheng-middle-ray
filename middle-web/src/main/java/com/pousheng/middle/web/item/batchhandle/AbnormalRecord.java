package com.pousheng.middle.web.item.batchhandle;

import lombok.Data;

/**
 * 异常记录
 */
@Data
public class AbnormalRecord {

    //编号
    private String code;

    //原因
    private String reason;

}
