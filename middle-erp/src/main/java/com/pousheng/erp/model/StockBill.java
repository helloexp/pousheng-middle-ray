package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口过来的库存单据
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Data
public class StockBill implements Serializable {
    private static final long serialVersionUID = -1588589765198821265L;

    /**
     * 单据编号
     */
    private String bill_no;

    /**
     * 账套 id
     */
    private String company_id;

    /**
     * 单据状态
     */
    private String bill_status;

    /**
     * 单据类型
     */
    private String bill_type;

    /**
     * 明细順序
     */
    private String sequence;


    /**
     * 仓库编号
     */
    private String stock_id;

    /**
     * 产品条码
     */
    private String barcode;


    /**
     * 这个单据引起库存变动的数量
     */
    private String quantity;

    /**
     * 修改时间
     */
    private Date modify_datetime;

    /**
     * 原始单号
     */
    private String original_bill_no;

}
