package com.pousheng.middle.web.events.warehouse;

import lombok.Getter;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 上午11:25
 */
public class StockRecordEvent implements Serializable {
    private static final long serialVersionUID = 919865508379615538L;


    @Getter
    private final Long shipmentId;

    @Getter
    private final String type;

    public StockRecordEvent(Long shipmentId, String type) {
        this.shipmentId = shipmentId;
        this.type = type;
    }
}
