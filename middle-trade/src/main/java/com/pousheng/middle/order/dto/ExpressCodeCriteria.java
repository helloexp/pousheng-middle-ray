package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

/**
 * Created by tony on 2017/6/28.
 */
@Data
public class ExpressCodeCriteria extends PagingCriteria implements java.io.Serializable{

    private static final long serialVersionUID = -2968622568681923595L;
    /**
     * 快递名称
     */
    private String name;
    /**
     * 恒康快递代码
     */
    private String hkCode;
    /**
     * 苏宁快递代码
     */
    private String suningCode;
    /**
     * 京东快递代码
     */
    private String jdCode;
    /**
     * 淘宝快递代码
     */
    private String taobaoCode;
    /**
     * 宝胜快递代码
     */
    private String poushengCode;
    /**
     * 分期乐快递代码
     */
    private String fenqileCode;

    /**
     *mpos快递代码
     */
    private String mposCode;

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

    /**
     * 官网快递代码
     */
    private String officalCode;

    /**
     * 官网快递代码
     */
    private String youzanCode;

    /**
     * 小红书code
     */
    private String xhsCode;


}
