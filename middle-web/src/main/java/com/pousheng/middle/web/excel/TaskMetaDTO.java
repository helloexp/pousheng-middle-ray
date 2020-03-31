package com.pousheng.middle.web.excel;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 14:27<br/>
 */
@Data
public class TaskMetaDTO implements Serializable {
    private static final long serialVersionUID = -4119518412018958038L;

    /**
     * 任务 ID
     */
    private Long id;
    /**
     * 原始 Excel 文件 url
     */
    private String filePath;
    /**
     * 原始 Excel 文件名
     */
    private String fileName;
    /**
     * 超时标记位，默认为0，如果最后执行时间超过5分钟没有更新，则加1，超过3次清理任务
     */
    private Integer timeout;
    /**
     * 是否被手动停止
     */
    private Integer manualStop;
    /**
     * 是否增量导入
     */
    private Boolean delta;

    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 最后执行时间
     */
    private Date updatedAt;
}
