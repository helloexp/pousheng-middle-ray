package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 宝胜仓库同步dto
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@Data
public class PoushengWarehouse implements Serializable {

    /**
     * 账套id, 也即是公司别
     */
    private String company_id;

    /**
     * 仓库内码
     */
    private String stock_id;

    /**
     * 仓库外码
     */
    private String stock_code;

    /**
     * 仓库名称
     */
    private String stock_name;

    /**
     * 电话
     */
    private String telphone;


    /**
     * 仓库地址
     */
    private String stock_address;

    /**
     * 仓库类型
     */
    private int stock_type;

    /**
     * 仓库修改日期
     */
    private Date modify_time;
}
