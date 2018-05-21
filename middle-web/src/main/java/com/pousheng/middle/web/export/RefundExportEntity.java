package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import com.pousheng.middle.web.utils.export.ExportDateFormat;
import lombok.Data;

import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/7/25.
 */
@Data
public class RefundExportEntity {

    /**
     * 订单号
     */
    @ExportTitle("订单号")
    private String orderCode;

    /**
     * 售后单号
     */
    @ExportTitle("售后单号")
    private String refundCode;

    /**
     * 售后子单号
     */
    @ExportTitle("售后单子单号")
    private String refundSubCode;

    /**
     * 外部单号
     */
    @ExportTitle("外部单号")
    private String outCode;

    /**
     * pos单号
     */
    @ExportTitle("pos单号")
    private String posCode;

    /**
     * 发货单号
     */
    @ExportTitle("发货单号")
    private String shipCode;

    /**
     * 交易单创建时间
     */
    @ExportTitle("交易单创建时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date payOrderCreateDate;

    /**
     * 交易单付款时间
     */
    @ExportTitle("交易单付款时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date payOrderPayDate;

    /**
     * 售后单创建时间
     */
    @ExportTitle("售后单创建时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date afterSaleCreateDate;


    /**
     * 售后单退货快递公司
     */
    @ExportTitle("售后单退货快递公司")
    private String afterSaleExpressCompany;


    /**
     * 售后单退货快递单号
     */
    @ExportTitle("售后单退货快递单号")
    private String afterSaleExpressNo;

    /**
     * 售后单退款时间
     */
    @ExportTitle("售后单退款时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date afterSaleRefundDate;

    /**
     * 店铺
     */
    @ExportTitle("店铺")
    private String shopName;


    @ExportTitle("客服订单备注")
    private String memo;

    @ExportTitle("售后类型")
    private String refundType;

    @ExportTitle("退款金额")
    private Double amt;

    @ExportTitle("状态")
    private String status;


    @ExportTitle("货号")
    private String materialCode;

    @ExportTitle("货品条码")
    private String itemNo;

    @ExportTitle("尺码")
    private String size;

    //@ExportTitle("skuCode")
    //private String skuCode;

    @ExportTitle("颜色")
    private String color;

    @ExportTitle("品牌")
    private String brand;

    //@ExportTitle("数量")
    //private Integer quantity;

    @ExportTitle("售后申请单数量")
    private Integer applyQuantity;

    @ExportTitle("实际入库数量")
    private Integer actualQuantity;

    @ExportTitle("总价")
    private Double totalPrice;

    @ExportTitle("入库时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date warehousingDate;
}
