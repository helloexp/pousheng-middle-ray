package com.pousheng.middle.web.item.batchhandle;

import lombok.Data;

import java.util.Date;

/**
 *批量导入,打标，取消打标记录
 */
@Data
public class BatchHandleRecord {

    //任务名称
    private String name;

    //创建时间
    private Date createAt;

    //状态
    private String state;

    //异常id
    private Long id;

    //类型
    private String type;

}
