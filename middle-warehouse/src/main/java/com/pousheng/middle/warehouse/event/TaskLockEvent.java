package com.pousheng.middle.warehouse.event;

import lombok.Getter;

import java.io.Serializable;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/9/13下午11:21
 */
public class TaskLockEvent implements Serializable {
    private static final long serialVersionUID = -2642344911815573344L;

    @Getter
    private final String context;

    public TaskLockEvent(String context) {
        this.context = context;
    }
}
