package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.util.List;

/**
 * 发货单信息
 * @author tanlongjun
 */
@Data
public class WmsShipmentInfo implements java.io.Serializable {

    /**
     * 公司代码
     */
    private String companycode;
    /**
     * 单号
     */
    private String billno;
    /**
     * EDI单据类型
     */
    private String billtype;
    /**
     * 手工单号
     */
    private String manualbillno;
    /**
     * 仓库代码
     */
    private String stockcode;
    /**
     * 客商编码
     */
    private String vendcustcode;
    /**
     * 目的仓编码
     */
    private String interstockcode;
    /**
     * 目的仓名称
     */
    private String interstockname;
    /**
     * 生效日期
     */
    private String billdate;
    /**
     * 下游单号
     */
    private String prefinishbillno;
    /**
     * 预计出/入库日期
     */
    private String expectdate;
    /**
     * 发运方式
     */
    private String transportmethodcode;
    /**
     * 发运方式
     */
    private String transportmethodname;
    /**
     * 单据审核时间
     */
    private String erpcheckdate;
    /**
     * 品牌
     */
    private String cardremark;
    /**
     * 承运商
     */
    private String freightcompany;

    /**
     * 托运单号
     */
    private String expressbillno;
    /**
     * 批次号
     */
    private String batchno;
    /**
     * 批次描述
     */
    private String batchmark;
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 区
     */
    private String area;
    /**
     * 地址
     */
    private String address;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 电话
     */
    private String phone;
    /**
     * 渠道代码
     */
    private String channelcode;
    /**
     * 备注
     */
    private String remark;

    /**
     * 单据总数
     */
    private int expectqty;


    /**
     * 明细
     */
    private List<WmsShipmentItem> ordersizes;

}
