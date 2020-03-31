package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

@Data
public class Items {
    //行号
    private String rowno;
    //公司内码
    private String companycode;
    //ERP单号
    private String billno;
    //sku，条码
    private String sku;
    //货号
    private String materialcode;
    //
    private String materialshortname;
    //尺码名称
    private String sizename;
    //预计数量
    private String expectqty;
    //网店交易单号
    private String shopbillno;
    //结算金额
    private String payamount;
    //零售价
    private String retailprice;
    //结算价
    private String balaprice;
}