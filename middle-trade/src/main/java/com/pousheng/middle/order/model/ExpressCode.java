package com.pousheng.middle.order.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tony on 2017/6/27.
 * Date:2017-06-27
 */
@Data
public class ExpressCode implements Serializable {


    private static final long serialVersionUID = -4973020601972118759L;

    private Long id;

    /**
     * 快递商名称
     */
    private String expressName;

    /**
     * 快递官方代码
     */
    private String officalExpressCode;
    /**
     * 宝胜官网快递代码
     */
    private String poushengExpressCode;
    /**
     * 京东快递代码
     */
    private String jdExpressCode;
    /**
     * 淘宝快递代码
     */
    private String taobaoExpressCode;
    /**
     * 苏宁快递代码
     */
    private String suningExpressCode;
    /**
     * 分期乐快递代码
     */
    private String fenqileExpressCode;
    /**
     * 恒康快递代码
     */
    private String hkExpressCode;

    private Date createdAt;

    private Date updatedAt;

}
