package com.pousheng.middle.web.export;

import com.pousheng.middle.web.utils.export.ExportDateFormat;
import com.pousheng.middle.web.utils.export.ExportTitle;
import lombok.Data;

import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/10/15.
 */
@Data
public class ShipmentExportEntity {

    @ExportTitle("店铺")
    private String shopName;

    @ExportTitle("订单号")
    private Long orderID;

    @ExportTitle("货品条码")
    private String itemNo;

    @ExportTitle("承运公司")
    private String shipmentCorpName;

    @ExportTitle("快递单号")
    private String carrNo;

    @ExportTitle("收件人")
    private String reciverName;

    @ExportTitle("收货地址")
    private String reciverAddress;

    @ExportTitle("电话")
    private String phone;

//    @ExportTitle("销售类型")
//    private String saleType;

    @ExportTitle("付款方式")
    private String payType;

    @ExportTitle("付款日期")
    @ExportDateFormat("yyyyMMdd")
    private Date paymentDate;

    @ExportTitle("数量")
    private Integer skuQuantity;

    @ExportTitle("金额")
    private Double fee;

//    @ExportTitle("运费")
//    private Double shipFee;

//    @ExportTitle("发票信息")
//    private String invoice;

    @ExportTitle("客服订单备注")
    private String orderMemo;

    @ExportTitle("交易状态")
    private String orderStatus;



    @ExportTitle("发货方式")
    private Integer shipWay;

}
