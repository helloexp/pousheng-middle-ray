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
    private String name;

    /**
     * 快递官方代码
     */
    private String officalCode;
    /**
     * 宝胜官网快递代码
     */
    private String poushengCode;
    /**
     * 京东快递代码
     */
    private String jdCode;
    /**
     * 淘宝快递代码
     */
    private String taobaoCode;
    /**
     * 苏宁快递代码
     */
    private String suningCode;
    /**
     * 分期乐快递代码
     */
    private String fenqileCode;

    /**
     *mpos快递代码
     */
    private String mposCode;
    /**
     * 恒康快递代码
     */
    private String hkCode;
    /**
     * 咕咚快递代码
     */
    private String codoonCode;
    /**
     * 考拉快递代码
     */
    private String kaolaCode;
    /**
     * 唯品会快递代码
     */
    private String vipCode;


    private Date createdAt;

    private Date updatedAt;

}
