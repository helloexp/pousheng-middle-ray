package com.pousheng.middle.web.export;

import com.pousheng.middle.web.utils.export.ExportDateFormat;
import com.pousheng.middle.web.utils.export.ExportTitle;
import lombok.Data;

import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Data
public class OrderExportEntity {


    /**
     * 订单编号
     */
    @ExportTitle("订单编号")
    private Long orderID;
    /**
     * 店铺名称
     */
    @ExportTitle("店铺名称")
    private String shopName;
    /**
     * 承运公司
     */
    @ExportTitle("承运公司")
    private String shipmentCorpName;

    /**
     * 快递单号
     */
    @ExportTitle("快递单号")
    private String carrNo;
    /**
     * 收件人
     */
    @ExportTitle("收件人")
    private String reciverName;
    /**
     * 收货地址
     */
    @ExportTitle("收货地址")
    private String reciverAddress;
    /**
     * 电话
     */
    @ExportTitle("电话")
    private String phone;
    /**
     * 付款方式
     */
    @ExportTitle("付款方式")
    private String payType;
    /**
     * 付款日期
     */
    @ExportTitle("付款日期")
    @ExportDateFormat("yyyyMMdd")
    private Date paymentDate;
    /**
     * 交易状态
     */
    @ExportTitle("交易状态")
    private String orderStatus;
    /**
     * 客服订单备注
     */
    @ExportTitle("客服订单备注")
    @ExportDateFormat("yyyy")
    private String orderMemo;

    /**
     * 运费
     */
    @ExportTitle("运费")
    private Double shipFee;


    /**
     * 发票信息
     */
    @ExportTitle("发票信息")
    private String invoice;

    /**
     * 货号
     */
    @ExportTitle("货号")
    private Long itemID;

    /**
     * 尺码
     */
    @ExportTitle("尺码")
    private String size;

    @ExportTitle("颜色")
    private  String color;

    /**
     * 品牌
     */
    @ExportTitle("品牌")
    private String brandName;

    /**
     * 数量
     */
    @ExportTitle("数量")
    private Integer skuQuantity;

    /**
     * 金额
     */
    @ExportTitle("金额")
    private Double fee;

    @ExportTitle("绩效店铺码")
    private String performanceShopCode;

    @ExportTitle("第三方或官网订单号")
    private String outId;
}
