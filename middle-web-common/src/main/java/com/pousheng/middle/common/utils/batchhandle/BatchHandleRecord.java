package com.pousheng.middle.common.utils.batchhandle;

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

    //url
    private String url;

    //信息
    private String message;

    //类型
    private String type;

}
