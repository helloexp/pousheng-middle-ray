package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * @author tanlongjun
 *
 */
@Data
public class WmsShipmentItem implements Serializable {

    private static final long serialVersionUID = 6697517974296687790L;
    /**
     * 条码
     */
    private String sku;
    /**
     * 货号
     */
    private String materialcode;
    /**
     * 尺码
     */
    private String sizename;
    /**
     * 预计数量
     */
    private int expectqty;
    /**
     * 上游来源单号
     */
    private String sourcebillno;

}
