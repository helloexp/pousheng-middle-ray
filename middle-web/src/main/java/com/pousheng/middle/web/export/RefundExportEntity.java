package com.pousheng.middle.web.export;

import com.pousheng.middle.web.utils.export.ExportDateFormat;
import com.pousheng.middle.web.utils.export.ExportTitle;
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
    private Long refundId;

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

    @ExportTitle("颜色")
    private String color;

    @ExportTitle("品牌")
    private String brand;

    @ExportTitle("售后申请单数量")
    private Integer applyQuantity;

    @ExportTitle("实际入库数量")
    private Integer actualQuantity;

    @ExportTitle("入库时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date warehousingDate;
}
