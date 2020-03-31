package com.pousheng.middle.web.task;

import lombok.Data;

import java.io.Serializable;

/**
 * 导入任务
 * Created by songrenfei on 2017/4/14
 */
@Data
public class SyncTask implements Serializable{


    private static final long serialVersionUID = 1950783941709625091L;


    /**
     * 处理状态 1 处理中 2 完成 -1失败
     */
    private Integer status;

    /**
     * 错误描述
     */
    private String error;

}
