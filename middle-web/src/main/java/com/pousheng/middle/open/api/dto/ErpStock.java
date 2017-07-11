package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * erp推送过来的库存情况
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-10
 */
@Data
public class ErpStock implements Serializable {
    private static final long serialVersionUID = -1707580245931795410L;

    /**
     * 公司编码
     */
    private String company_id;

    /**
     * 仓库id
     */
    private String stock_id;

    /**
     * 条码
     */
    private String barcode;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 更新时间
     */
    private Date modify_time;
}
